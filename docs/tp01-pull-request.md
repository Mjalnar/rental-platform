# TP 01 : une CI observable et bloquante

Branche `tp/01-ci`, partie de `checkpoint/00-starter`.

## Résumé

- Chaque service a **trois jobs** : `tests` et `quality` en parallèle, puis
  `Docker image` (`needs: [tests, quality]`).
- Les portes qualité sont configurées dans le dépôt : **Checkstyle** pour Java
  (`config/checkstyle/checkstyle.xml`) et **Ruff** pour Python (`ruff.toml`,
  familles `E`, `F`, `I` et `ruff format --check`).
- Les **rapports** (JUnit, Pytest, Checkstyle, Ruff, Gitleaks) sont téléversés
  avec `if: always()` et gardés 7 jours.
- Le workflow **Secret detection** passe Gitleaks sur l'historique complet.
- `python scripts/course.py verify` reproduit en local les portes tests et qualité.

## Lecture des workflows de départ

Chaque service avait un seul job `build` (tests puis image), déclenché sur tous les `push` et `pull_request`.

| Job | Risque couvert | Entrées | Sortie | Coût approx. |
|---|---|---|---|---|
| `Java service / build` | Régression fonctionnelle Java ; Dockerfile cassé | Code, JDK 21, dépendances Maven (sans cache) | Statut vert/rouge, image locale jetée | ~3 à 4 min (Gradle téléchargé à chaque fois) |
| `Python service / build` | Régression fonctionnelle Python ; Dockerfile cassé | Code, Python 3.12, `pip install` (sans cache) | Statut vert/rouge, image locale jetée | ~1 à 2 min |

Limites constatées :

- aucun contrôle de style ;
- aucun rapport conservé ;
- l'image est construite même quand les tests échouent ;
- pas de timeout, ce qui laisse un job bloqué jusqu'à 6 h ;
- permissions par défaut du jeton ;
- pas d'annulation des exécutions obsolètes ;
- double exécution `push` + `pull_request` sur chaque branche.

## Étape 1 : observer un échec

Commits `test: break pricing assertion…` puis `test: restore pricing assertion…`.

> Les réponses ci-dessous décrivent le comportement attendu. Les durées
> exactes sont à relever dans l'onglet *Actions* de la PR.

1. **Premier job en échec** : `Python service / build`, à l'étape `Tests`.
   Le workflow Java reste vert, car les deux workflows sont indépendants.
2. **Ligne qui donne la cause** : la sortie Pytest
   `E   AssertionError: assert 120.0 == 999` ainsi que
   `FAILED tests/test_pricing.py::test_standard_two_days_costs_120_eur`.
3. **Rapport structuré** : avec le workflow de départ, **non**. Il n'y a que le
   log brut, et l'étape `Docker image` est sautée. Après l'étape 4, l'artefact
   `python-test-reports` (`pytest.xml`) reste disponible.
4. **Délai de retour** : environ ___ min (à relever). Il se compose de la file
   d'attente du runner, de `pip install` sans cache et de Pytest.

## Choix de conception

- **Graphe** `tests ─┐ ├─> image ; quality ─┘` : l'image, qui est l'étape la plus
  coûteuse et la seule qui produit un livrable, n'est construite que si le code
  est testé et conforme.
- **Timeouts explicites** : 15 min pour les tests Java, 10 min pour la qualité,
  20 min pour l'image Java et 15 min pour l'image Python. Un job bloqué est
  coupé au lieu de consommer des minutes.
- **Permissions** : `contents: read` au niveau du workflow. Seul le job image
  demande `packages: write`, et il ne s'en sert que sur `main`.
- **Cache** : `gradle/actions/setup-gradle` (dépendances et wrapper),
  `setup-python` avec `cache: pip` indexé sur `requirements-dev.txt`, et le cache
  BuildKit `type=gha` pour les couches Docker.
- **Concurrence** : groupe `workflow + numéro de PR (ou ref)` avec
  `cancel-in-progress: true`. Un nouveau push annule l'exécution devenue obsolète.
- **Aucune publication depuis une PR** : `docker/login-action` et `push:` sont
  conditionnés à `github.event_name == 'push' && github.ref == 'refs/heads/main'`.
  Sur une PR, l'image est construite (le Dockerfile est donc validé) mais pas poussée.
- **`push` limité à `main`** : les branches sont couvertes par `pull_request`.
  Cela évite de lancer deux fois les mêmes contrôles.

### Pourquoi pas de filtre `paths` sur `pull_request` ?

Un contrôle requis par la protection de branche doit **rapporter un statut**.
Avec `paths: [apps/pricing-service/**]`, une PR qui ne touche que le service Java
ne déclenche pas le workflow Python. Les contrôles `Python tests`, `Python quality`
et `Python Docker image` restent alors en *Expected — Waiting for status to be
reported*, et la PR n'est **jamais fusionnable**. Inversement, une PR qui ne
modifie qu'un fichier partagé (`docker-compose.yml`, `scripts/`, un workflow)
pourrait échapper aux tests. Pour ne reconstruire qu'un service, il faut garder
le workflow déclenché et filtrer **à l'intérieur** du job (par exemple avec
`dorny/paths-filter`, puis des étapes conditionnelles qui sortent en succès),
de sorte que le contrôle soit toujours présent.

### Exceptions locales aux règles

Il n'y a aucune exception aujourd'hui. Si une exception devient nécessaire, elle
reste locale et justifiée par un commentaire :
`@SuppressWarnings("checkstyle:RuleName")` (rendu possible par
`SuppressWarningsHolder`) ou `# noqa: XXX` côté Ruff. Aucune règle n'est
désactivée globalement.

## Étape 5 : détection de secrets

Le scan porte sur `fetch-depth: 0`, donc sur tous les commits et pas seulement
sur le diff. Le binaire Gitleaks est épinglé (8.30.1) et le rapport SARIF est
expurgé (`--redact`). L'allowlist se limite à `gradle-wrapper.jar`.

Test à faire avec la **valeur factice fournie par l'enseignant**, sur une branche
jetable : le job `Secret detection` doit échouer. Supprimez ensuite cette branche
selon ses consignes.

| Action | Effet | Suffisant ? |
|---|---|---|
| **Supprimer la ligne fautive** | Le secret disparaît de la version courante, mais il reste dans l'historique, les forks, les clones et les caches. Gitleaks le détecte encore sur l'historique. | Non, si le secret a été poussé. |
| **Révoquer le secret** | Le secret devient inutilisable chez le fournisseur ; on en émet un nouveau, stocké dans un gestionnaire de secrets. | **Oui, c'est la priorité absolue** dès qu'un secret a été exposé. |
| **Réécrire l'historique** (`git filter-repo`, BFG) | Le secret disparaît des commits, mais les SHA changent : push forcé, coordination de l'équipe, et les forks et clones existants le conservent. | Complément de nettoyage, jamais un substitut à la révocation. |

## Étape 7 : protéger `main` (après validation de l'enseignant)

*Settings → Branches → Add rule* (ou *Rulesets*) sur `main` :

- *Require a pull request before merging* ;
- *Require status checks to pass*, avec les contrôles `Java tests`,
  `Java quality`, `Java Docker image`, `Python tests`, `Python quality`,
  `Python Docker image` et `Secret detection` ;
- *Require branches to be up to date before merging*.

Vérifications : une PR rouge doit être bloquée (bouton *Merge* désactivé) et une
PR verte doit être fusionnable.

## Validation locale

```bash
python scripts/course.py verify   # tests Java + Python, Checkstyle, Ruff → code 0
```
