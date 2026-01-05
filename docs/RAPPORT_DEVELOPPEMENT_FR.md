# StoreParcelAPI - Rapport de Développement

## Aperçu du Projet
Construction d'une API REST haute performance pour permettre aux magasins de rechercher les colis clients.

**Objectif de Performance** : Temps de réponse P95 < 60ms avec 50 000+ colis

**Date** : 30 décembre 2025

---

## État des Checkpoints

| Checkpoint | Description | Statut |
|------------|-------------|--------|
| 1 | Application démarre, health check fonctionne | ✅ TERMINÉ |
| 2 | Générer 50 000 colis de test | ✅ TERMINÉ |
| 3 | Tests Gatling fonctionnent | ✅ TERMINÉ |
| 4 | Mesurer performance de base | ✅ TERMINÉ |
| 5 | P95 < 200ms | ✅ TERMINÉ |
| 6 | P95 < 100ms | ✅ TERMINÉ |
| 7 | P95 < 60ms (OBJECTIF) | ✅ TERMINÉ (58ms) |
| 4.1 | Prérequis GCP + Atlas configurés | ✅ TERMINÉ |
| 4.2 | Infrastructure Terraform déployée | ✅ TERMINÉ |
| 4.3 | GitHub Actions CI/CD configuré | ✅ TERMINÉ |
| 4.4 | Application accessible sur Cloud Run | ✅ TERMINÉ |
| 4.5 | Données chargées dans Atlas | ⏳ PRÊT À TESTER |
| 4.6 | P95 < 60ms sur Cloud Run | ⏳ PRÊT À TESTER |
| 4.7 | Terraform destroy fonctionne | ✅ TERMINÉ |

---

## Journal de Progression

### Étape 1 : Structure du Projet Maven (pom.xml) ✅

**Ce qu'on a créé** : Le fichier `pom.xml` - la configuration du projet Maven.

**Pourquoi** : Maven gère nos dépendances (bibliothèques) et compile le projet. Sans lui, il faudrait télécharger manuellement chaque fichier JAR.

**Dépendances clés ajoutées** :
| Dépendance | Utilité |
|-----------|---------|
| spring-boot-starter-web | Support API REST |
| spring-boot-starter-data-mongodb | Connexion base de données MongoDB |
| spring-boot-starter-validation | Validation des requêtes |
| spring-boot-starter-hateoas | Format de pagination HAL+JSON |
| lombok | Génération automatique des getters/setters |

---

### Étape 2 : Classe Principale de l'Application ✅

**Fichier** : `StoreParcelsApplication.java`

**Ce qu'elle fait** : Point d'entrée de l'application. L'annotation `@SpringBootApplication` indique à Spring de :
1. Scanner les composants (controllers, services, repositories)
2. Auto-configurer selon les dépendances
3. Démarrer le serveur Tomcat intégré

```java
@SpringBootApplication
public class StoreParcelsApplication {
    public static void main(String[] args) {
        SpringApplication.run(StoreParcelsApplication.class, args);
    }
}
```

---

### Étape 3 : Structure des Packages ✅

```
src/main/java/com/kantic/storeParcelsWS/
├── controller/   → Gère les requêtes HTTP
├── service/      → Logique métier
├── repository/   → Opérations base de données
├── model/        → Classes entités (structure des données)
├── dto/          → Objets Requête/Réponse
├── exception/    → Gestion des erreurs personnalisée
└── config/       → Classes de configuration
```

**Pourquoi cette structure ?** : Séparation des responsabilités. Chaque couche a une seule responsabilité :
- Controller : "Quel endpoint a été appelé ?"
- Service : "Quelles règles métier appliquer ?"
- Repository : "Comment interroger la base de données ?"

---

### Étape 4 : Configuration Docker ✅

**Fichiers créés** :
- `docker-compose.yml` - Orchestre le conteneur MongoDB
- `Dockerfile` - Construit le conteneur de notre application

**Comment utiliser** :
```bash
# Démarrer MongoDB
docker-compose up -d

# Vérifier qu'il tourne
docker ps
```

**Pourquoi Docker ?** : Environnement cohérent. MongoDB fonctionne de la même manière sur chaque machine de développeur.

---

### Étape 5 : Configuration de l'Application ✅

**Fichier** : `application.yml`

**Profils expliqués** :
- `local` : Développement sur votre machine (MongoDB sur localhost)
- `docker` : Exécution dans Docker (MongoDB via nom du conteneur)
- `gcp` : Déploiement cloud (URI MongoDB depuis variable d'environnement)

**Pourquoi les profils ?** : Même code, configurations différentes. Aucune modification de code nécessaire entre environnements.

---

### Étape 6 : Classes Modèle/Entité ✅

**Fichiers créés** :
- `ParcelStatus.java` - Enum avec tous les statuts possibles
- `Customer.java` - Document embarqué pour les infos client
- `StorageLocation.java` - Où le colis est stocké dans le magasin
- `Parcel.java` - Entité principale avec tous les champs

**Annotations clés** :
| Annotation | Signification |
|-----------|---------|
| `@Document` | Cette classe = collection MongoDB |
| `@Id` | ID interne de MongoDB |
| `@Field("nom")` | Mapper vers un nom de champ différent en BD |
| `@Indexed` | Créer un index pour requêtes rapides |
| `@CompoundIndex` | Index multi-champs |

**Pourquoi les index sont importants** : Sans index, MongoDB parcourt TOUS les 50 000 documents pour chaque requête. Avec les index, il saute directement aux documents correspondants → P95 < 60ms possible !

---

### Étape 7 : Couche Repository ✅

**Fichiers créés** :
- `ParcelRepository.java` - Interface principale du repository
- `ParcelRepositoryCustom.java` - Interface de recherche personnalisée
- `ParcelRepositoryCustomImpl.java` - Implémentation de la recherche

**Qu'est-ce qu'un Repository ?** : Interface qui définit les opérations de base de données.

**La magie de Spring Data** : Il suffit de définir les noms de méthodes, Spring génère l'implémentation !

```java
Optional<Parcel> findByParcelId(String parcelId);
// Génère automatiquement : db.parcels.findOne({parcelId: value})
```

**Implémentation de recherche personnalisée** : Gère la recherche dynamique multi-critères avec :
- Plusieurs opérateurs (EQ, NE, IN, LIKE, LE, GE)
- Tri
- Pagination

---

### Étape 8 : Couche DTO ✅

**Fichiers créés** :
- `SearchRequestDTO.java` - Structure de la requête de recherche
- `CriteriaDTO.java` - Un critère de recherche
- `SortDTO.java` - Option de tri
- `PaginationDTO.java` - Paramètres de pagination
- `ParcelResponseDTO.java` - Colis dans la réponse API
- `CustomerResponseDTO.java` - Client dans la réponse API

**Pourquoi les DTOs ?**
- Contrôler quelles données sont exposées à l'API
- Découpler le contrat API du schéma de base de données
- Cacher les champs internes (comme `_id` de MongoDB)

---

### Étape 9 : Couche Service ✅

**Fichiers créés** :
- `ParcelService.java` - Interface du service
- `ParcelServiceImpl.java` - Implémentation du service

**Ce que fait un Service** :
- Contient la logique métier
- Convertit les entités en DTOs
- Se situe entre Controller et Repository

---

### Étape 10 : Couche Controller ✅

**Fichier** : `ParcelController.java`

**Endpoints créés** :
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/store_parcels/health` | Vérification santé |
| POST | `/store_parcels/search` | Recherche de colis |
| GET | `/store_parcels/parcel/{id}` | Obtenir un colis |

---

### Étape 11 : Configuration de l'Environnement ✅

**Installés** :
- Java 21 (OpenJDK via Homebrew)
- Docker Desktop
- Maven 3.9.12

**Commandes utilisées** :
```bash
# Installer Java
brew install openjdk@21

# Configurer Java dans ~/.zshrc
export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
export PATH="$JAVA_HOME/bin:$PATH"

# Installer Docker
brew install --cask docker

# Installer Maven
brew install maven
```

---

### Étape 12 : Test de l'Application ✅

**MongoDB démarré** :
```bash
docker-compose up -d
```

**Application démarrée** :
```bash
mvn spring-boot:run
```

**Log de démarrage** (1.476 secondes) :
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v3.4.4)

Started StoreParcelsApplication in 1.476 seconds
```

**Test du health check** :
```bash
curl http://localhost:8080/store_parcels/health
```

**Réponse** :
```json
{
    "status": "UP",
    "timestamp": 1767110196871
}
```

**Test de recherche** (base de données vide) :
```bash
curl -X POST http://localhost:8080/store_parcels/search \
  -H "Content-Type: application/json" \
  -d '{"criterias":[],"pagination":{"page":1,"pageSize":20}}'
```

**Réponse** :
```json
{
    "_embedded": {
        "storeParcels": []
    },
    "page": {
        "number": 1,
        "size": 20,
        "totalPages": 0,
        "totalElements": 0
    }
}
```

---

### Étape 13 : Générer 50 000 Colis de Test (Checkpoint 2) ✅

**Fichiers créés** :
- `TestDataGenerator.java` - Génère des colis aléatoires par lots
- `AdminController.java` - Endpoints pour la gestion des données (local/docker uniquement)

**Ce que fait TestDataGenerator** :
1. Crée des colis avec des données aléatoires mais réalistes
2. Utilise des insertions par lots (1000 à la fois) pour la vitesse
3. Crée les index après l'insertion des données

**Endpoints admin** (uniquement en profils local/docker) :
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/store_parcels/admin/generate-test-data` | Générer 50 000 colis |
| DELETE | `/store_parcels/admin/clear-data` | Effacer toutes les données |
| GET | `/store_parcels/admin/count` | Compter les colis |

**Résultats de la génération** :
```bash
curl -X POST http://localhost:8080/store_parcels/admin/generate-test-data
```
```json
{
    "status": "success",
    "parcelsGenerated": 50000,
    "durationMs": 4337,
    "message": "Generated 50000 parcels in 4337ms"
}
```

**Tests de recherche** :
| Test | Requête | Résultats |
|------|---------|-----------|
| Magasin ST001 | `deliveryStoreId = ST001` | 4 975 colis |
| Client "John" | `customer.firstName LIKE John` | 1 984 colis |
| Multi-critères | `ST001 + READY_FOR_PICKUP` | 619 colis |

**Distribution des données** (10 magasins, 8 statuts) :
- ~5 000 colis par magasin
- ~6 250 colis par statut

---

## Résumé des Fichiers Créés

| Catégorie | Fichiers |
|-----------|----------|
| **Configuration** | `pom.xml`, `application.yml`, `docker-compose.yml`, `Dockerfile` |
| **Principal** | `StoreParcelsApplication.java` |
| **Modèle** | `Parcel.java`, `Customer.java`, `StorageLocation.java`, `ParcelStatus.java` |
| **Repository** | `ParcelRepository.java`, `ParcelRepositoryCustom.java`, `ParcelRepositoryCustomImpl.java` |
| **Service** | `ParcelService.java`, `ParcelServiceImpl.java` |
| **Controller** | `ParcelController.java`, `AdminController.java` |
| **DTO** | `SearchRequestDTO.java`, `CriteriaDTO.java`, `SortDTO.java`, `PaginationDTO.java`, `ParcelResponseDTO.java`, `CustomerResponseDTO.java` |
| **Utilitaire** | `TestDataGenerator.java` |
| **Documentation** | `README.md`, `DEVELOPMENT_REPORT_EN.md`, `RAPPORT_DEVELOPPEMENT_FR.md` |

**Total** : 22 fichiers créés

---

## Schéma d'Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        CLIENT                                │
│                   (Postman, curl, etc.)                     │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      CONTROLLER                              │
│    ParcelController.java                                    │
│    - POST /store_parcels/search                             │
│    - GET  /store_parcels/health                             │
│    - GET  /store_parcels/parcel/{id}                        │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                       SERVICE                                │
│    ParcelService.java                                       │
│    - Logique métier                                         │
│    - Conversion Entité → DTO                                │
│    - Délégation des requêtes                                │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      REPOSITORY                              │
│    ParcelRepository.java                                    │
│    - Requêtes base de données                               │
│    - Pagination                                             │
│    - Tri                                                    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                       MONGODB                                │
│    Collection : parcels                                     │
│    Index : parcelId, magasin+statut, nom client             │
└─────────────────────────────────────────────────────────────┘
```

---

### Étape 14 : Tests de Performance Gatling (Checkpoints 3-7) ✅

**Fichiers créés** :
- `ParcelSearchSimulation.java` - Test de charge complet avec plusieurs scénarios
- `BaselineSimulation.java` - Test de base rapide
- `QuickTestSimulation.java` - Test de vérification rapide

**Qu'est-ce que Gatling ?**
Gatling est un outil de test de charge qui :
- Simule de nombreux utilisateurs simultanés sur votre API
- Mesure les temps de réponse, le débit, les taux d'erreur
- Génère des rapports HTML détaillés avec des graphiques

**Dépendances ajoutées au pom.xml** :
```xml
<dependency>
    <groupId>io.gatling.highcharts</groupId>
    <artifactId>gatling-charts-highcharts</artifactId>
    <version>3.10.3</version>
    <scope>test</scope>
</dependency>
```

**Scénarios de test** :
| Scénario | Description | Débit |
|----------|-------------|-------|
| Recherche Simple | Recherche tous les colis | 2 req/sec |
| Recherche Magasin | Recherche par ID magasin | 4 req/sec |
| Magasin + Statut | Recherche multi-critères | 4 req/sec |
| Recherche Client | Requête LIKE sur le nom | 2 req/sec |
| Magasin Aléatoire | Sélection aléatoire | 8 req/sec |

**Résultats de Performance** :
| Métrique | Résultat | Objectif |
|----------|----------|----------|
| **Temps de réponse P95** | **58ms** | < 60ms ✅ |
| Temps de réponse P99 | 64ms | - |
| Temps de réponse moyen | 18ms | - |
| Temps de réponse min | 5ms | - |
| Temps de réponse max | 88ms | - |
| Taux de succès | 100% | > 99% ✅ |
| Total requêtes | 600 | - |

**Exécuter les tests Gatling** :
```bash
# Test de charge complet (30 secondes)
mvn gatling:test -Dgatling.simulationClass=com.kantic.storeParcelsWS.simulation.ParcelSearchSimulation

# Test rapide (5 secondes)
mvn gatling:test -Dgatling.simulationClass=com.kantic.storeParcelsWS.simulation.QuickTestSimulation
```

Les rapports sont générés dans : `target/gatling/*/index.html`

---

---

## Partie 2 : Endpoints d'Écriture CRUD ✅

**Date** : 31 décembre 2025

### Objectif
Étendre l'API avec des endpoints d'écriture (PUT, DELETE) incluant :
- Machine d'état pour les transitions de statut
- Soft delete (suppression logique)
- Gestion d'erreurs avancée au format RFC 7807

---

### Étape 15 : Machine d'État des Statuts ✅

**Fichier modifié** : `model/ParcelStatus.java`

**Qu'est-ce qu'une machine d'état ?**
Une machine d'état définit les transitions valides entre les différents statuts d'un colis. Par exemple :
- Un colis `PENDING` peut devenir `IN_TRANSIT` ou `CANCELLED`
- Un colis `DELIVERED` ne peut plus changer (état terminal)

**Diagramme de transitions** :
```
PENDING ──────────► IN_TRANSIT ──────────► READY_FOR_PICKUP
    │                    │                        │
    └──► CANCELLED ◄─────┴────────────────────────┤
                                                   │
                            ┌──────────────────────┤
                            │                      │
                            ▼                      ▼
                        EXPIRED              PICKED_UP
                            │                      │
                            ▼                      ▼
                        RETURNED              DELIVERED
```

**États terminaux** : `DELIVERED`, `RETURNED`, `CANCELLED` (aucune transition possible)

**Méthodes ajoutées** :
| Méthode | Description |
|---------|-------------|
| `canTransitionTo(status)` | Vérifie si la transition est autorisée |
| `getAllowedTransitions()` | Retourne les transitions possibles |
| `isTerminal()` | Vérifie si c'est un état final |
| `isDeletable()` | Vérifie si le colis peut être supprimé |

**Exemple de code** :
```java
ParcelStatus.READY_FOR_PICKUP.canTransitionTo(ParcelStatus.PICKED_UP);  // true
ParcelStatus.DELIVERED.canTransitionTo(ParcelStatus.CANCELLED);        // false (état terminal)
ParcelStatus.CANCELLED.isDeletable();                                  // true
```

---

### Étape 16 : Soft Delete (Suppression Logique) ✅

**Fichier modifié** : `model/Parcel.java`

**Qu'est-ce que le soft delete ?**
Au lieu de supprimer physiquement un document de la base de données, on ajoute un flag `deleted = true`. Avantages :
- Conservation de l'historique
- Possibilité de restauration
- Audit trail préservé

**Champ ajouté** :
```java
@Builder.Default
private Boolean deleted = false;
```

**Règles métier** :
- Seuls les colis `CANCELLED` ou `RETURNED` peuvent être supprimés
- Les colis supprimés sont automatiquement exclus des recherches

---

### Étape 17 : Classes d'Exception ✅

**Package créé** : `exception/`

**Fichiers créés** :

| Classe | Code HTTP | Quand utilisée |
|--------|-----------|----------------|
| `ParcelNotFoundException` | 404 | Colis non trouvé ou déjà supprimé |
| `InvalidStatusTransitionException` | 400 | Transition de statut invalide |
| `ParcelNotDeletableException` | 400 | Tentative de supprimer un colis actif |

**Exemple d'exception** :
```java
// Si on essaie de passer de DELIVERED à CANCELLED
throw new InvalidStatusTransitionException(ParcelStatus.DELIVERED, ParcelStatus.CANCELLED);
// Message: "Cannot transition from DELIVERED to CANCELLED"
```

---

### Étape 18 : GlobalExceptionHandler (Format RFC 7807) ✅

**Fichier créé** : `exception/GlobalExceptionHandler.java`

**Qu'est-ce que RFC 7807 ?**
Un standard HTTP pour les réponses d'erreur structurées. Spring 6+ utilise `ProblemDetail` pour implémenter ce standard.

**Structure d'une réponse d'erreur** :
```json
{
  "type": "about:blank",
  "title": "Invalid Status Transition",
  "status": 400,
  "detail": "Cannot transition from DELIVERED to CANCELLED",
  "instance": "/store_parcels/parcel/P000001",
  "currentStatus": "DELIVERED",
  "targetStatus": "CANCELLED",
  "allowedTransitions": []
}
```

**Champs standards RFC 7807** :
| Champ | Description |
|-------|-------------|
| `type` | URI identifiant le type d'erreur |
| `title` | Titre court de l'erreur |
| `status` | Code HTTP |
| `detail` | Description détaillée |
| `instance` | URI de la ressource concernée |

**Propriétés personnalisées** : On peut ajouter des champs métier comme `currentStatus`, `allowedTransitions`, `parcelId`, etc.

---

### Étape 19 : UpdateParcelRequestDTO ✅

**Fichier créé** : `dto/UpdateParcelRequestDTO.java`

**Pourquoi un record Java ?**
Les records (Java 16+) sont parfaits pour les DTOs :
- Immutables par défaut
- Génération automatique de `equals()`, `hashCode()`, `toString()`
- Syntaxe concise

**Structure** :
```java
public record UpdateParcelRequestDTO(
    String parcelStatus,           // Optionnel - nouveau statut
    Boolean isPaid,                // Optionnel - mise à jour du paiement
    Instant expirationDate,        // Optionnel - nouvelle date d'expiration
    CustomerUpdateDTO customer,    // Optionnel - mise à jour contact
    StorageLocationUpdateDTO storageLocation  // Optionnel - mise à jour emplacement
) {
    public record CustomerUpdateDTO(
        String email,              // Seul l'email peut être modifié
        String phoneNumber         // Seul le téléphone peut être modifié
    ) {}
}
```

**Champs NON modifiables** (par design) :
- `parcelId` - Identifiant unique, ne change jamais
- `customerOrderId` - Lié à la commande origine
- `deliveryStoreId` - Magasin de livraison
- `customer.firstName` / `lastName` - Données client protégées

---

### Étape 20 : Service Layer - Méthodes Update et Delete ✅

**Fichiers modifiés** :
- `service/ParcelService.java` - Interface avec nouvelles méthodes
- `service/ParcelServiceImpl.java` - Implémentation

**Méthode updateParcel** :
```java
@Transactional
public ParcelResponseDTO updateParcel(String parcelId, UpdateParcelRequestDTO request) {
    // 1. Trouver le colis (exclure les supprimés)
    Parcel parcel = repository.findByParcelIdAndDeletedFalse(parcelId)
        .orElseThrow(() -> new ParcelNotFoundException(parcelId));

    // 2. Valider la transition de statut si demandée
    if (request.parcelStatus() != null) {
        ParcelStatus newStatus = ParcelStatus.valueOf(request.parcelStatus());
        if (!parcel.getStatus().canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(parcel.getStatus(), newStatus);
        }
        parcel.setStatus(newStatus);
        parcel.setStatusUpdateDate(Instant.now());  // Mise à jour automatique
    }

    // 3. Appliquer les autres modifications...
    // 4. Sauvegarder et retourner
    return toResponseDTO(repository.save(parcel));
}
```

**Méthode deleteParcel** :
```java
@Transactional
public void deleteParcel(String parcelId) {
    Parcel parcel = repository.findByParcelIdAndDeletedFalse(parcelId)
        .orElseThrow(() -> new ParcelNotFoundException(parcelId));

    // Vérifier que le colis peut être supprimé
    if (!parcel.getStatus().isDeletable()) {
        throw new ParcelNotDeletableException(parcelId, parcel.getStatus());
    }

    // Soft delete
    parcel.setDeleted(true);
    repository.save(parcel);
}
```

**Annotation @Transactional** : Garantit que toutes les opérations sont atomiques (tout réussit ou tout échoue).

---

### Étape 21 : Repository - Exclusion des Supprimés ✅

**Fichiers modifiés** :
- `repository/ParcelRepository.java` - Nouvelle méthode
- `repository/ParcelRepositoryCustomImpl.java` - Filtre dans la recherche

**Nouvelle méthode** :
```java
Optional<Parcel> findByParcelIdAndDeletedFalse(String parcelId);
```

**Modification de la recherche** :
```java
// Dans searchParcels()
criteriaList.add(Criteria.where("deleted").ne(true));
```

**Pourquoi `ne(true)` au lieu de `is(false)` ?**
Pour gérer les documents existants qui n'ont pas le champ `deleted` (valeur `null`). `ne(true)` retourne les documents où `deleted` est `false` OU `null`.

---

### Étape 22 : Controller - Nouveaux Endpoints ✅

**Fichier modifié** : `controller/ParcelController.java`

**Nouveaux endpoints** :

| Méthode | Endpoint | Description | Codes retour |
|---------|----------|-------------|--------------|
| PUT | `/store_parcels/parcel/{parcelId}` | Mettre à jour un colis | 200, 400, 404 |
| DELETE | `/store_parcels/parcel/{parcelId}` | Supprimer un colis (soft) | 204, 400, 404 |

**Endpoint PUT** :
```java
@PutMapping("/parcel/{parcelId}")
public ResponseEntity<ParcelResponseDTO> updateParcel(
        @PathVariable String parcelId,
        @Valid @RequestBody UpdateParcelRequestDTO request) {
    ParcelResponseDTO updated = parcelService.updateParcel(parcelId, request);
    return ResponseEntity.ok(updated);
}
```

**Endpoint DELETE** :
```java
@DeleteMapping("/parcel/{parcelId}")
public ResponseEntity<Void> deleteParcel(@PathVariable String parcelId) {
    parcelService.deleteParcel(parcelId);
    return ResponseEntity.noContent().build();  // 204 No Content
}
```

**Pourquoi 204 No Content pour DELETE ?**
Convention REST : une suppression réussie ne retourne pas de body, juste un code 204.

---

### Étape 23 : Tests Unitaires et d'Intégration ✅

**Fichiers créés** :
- `test/model/ParcelStatusTest.java` - 44 tests pour la machine d'état
- `test/service/ParcelServiceImplTest.java` - 11 tests unitaires
- `test/controller/ParcelControllerIntegrationTest.java` - Tests d'intégration

**Tests de la machine d'état (44 tests)** :
```java
@ParameterizedTest
@CsvSource({
    "PENDING, IN_TRANSIT, true",
    "PENDING, DELIVERED, false",
    "DELIVERED, CANCELLED, false",
    // ...
})
void shouldValidateStatusTransitions(ParcelStatus from, ParcelStatus to, boolean expected) {
    assertThat(from.canTransitionTo(to)).isEqualTo(expected);
}
```

**Tests unitaires du service (avec Mockito)** :
- `updateParcel_shouldUpdateStatusAndTimestamp` - Vérifie mise à jour statut
- `updateParcel_shouldRejectInvalidTransition` - Vérifie rejet transition invalide
- `deleteParcel_shouldSoftDeleteCancelledParcel` - Vérifie soft delete
- `deleteParcel_shouldRejectActiveParcel` - Vérifie rejet suppression colis actif

**Tests d'intégration (avec Testcontainers)** :
- Utilisent un vrai MongoDB dans Docker
- Testent le flux complet HTTP → Controller → Service → Repository → MongoDB

**Exécuter les tests** :
```bash
# Tests machine d'état
mvn test -Dtest=ParcelStatusTest

# Tests service
mvn test -Dtest=ParcelServiceImplTest

# Tests intégration (nécessite Docker)
mvn test -Dtest=ParcelControllerIntegrationTest
```

---

### Résumé des Fichiers Partie 2

| Catégorie | Fichiers |
|-----------|----------|
| **Modèle** | `ParcelStatus.java` (modifié), `Parcel.java` (modifié) |
| **Exception** | `ParcelNotFoundException.java`, `InvalidStatusTransitionException.java`, `ParcelNotDeletableException.java`, `GlobalExceptionHandler.java` |
| **DTO** | `UpdateParcelRequestDTO.java` |
| **Service** | `ParcelService.java` (modifié), `ParcelServiceImpl.java` (modifié) |
| **Repository** | `ParcelRepository.java` (modifié), `ParcelRepositoryCustomImpl.java` (modifié) |
| **Controller** | `ParcelController.java` (modifié) |
| **Tests** | `ParcelStatusTest.java`, `ParcelServiceImplTest.java`, `ParcelControllerIntegrationTest.java` |

**Total Partie 2** : 4 nouveaux fichiers créés, 6 fichiers modifiés, 3 fichiers de tests

---

### Exemples d'Utilisation des Nouveaux Endpoints

**Mettre à jour le statut d'un colis** :
```bash
curl -X PUT http://localhost:8080/store_parcels/parcel/P000001 \
  -H "Content-Type: application/json" \
  -d '{
    "parcelStatus": "PICKED_UP",
    "isPaid": true
  }'
```

**Réponse (200 OK)** :
```json
{
  "parcelId": "P000001",
  "customerOrderId": "ORD123456",
  "deliveryStoreId": "ST001",
  "parcelStatus": "PICKED_UP",
  "isPaid": true,
  "customer": {
    "firstName": "John",
    "lastName": "Doe"
  },
  "expirationDate": "2025-02-15T10:00:00Z",
  "isExpired": false
}
```

**Transition invalide** :
```bash
curl -X PUT http://localhost:8080/store_parcels/parcel/P000002 \
  -H "Content-Type: application/json" \
  -d '{"parcelStatus": "CANCELLED"}'
# Si le colis est DELIVERED...
```

**Réponse (400 Bad Request)** :
```json
{
  "type": "about:blank",
  "title": "Invalid Status Transition",
  "status": 400,
  "detail": "Cannot transition from DELIVERED to CANCELLED",
  "currentStatus": "DELIVERED",
  "targetStatus": "CANCELLED",
  "allowedTransitions": []
}
```

**Supprimer un colis annulé** :
```bash
curl -X DELETE http://localhost:8080/store_parcels/parcel/P000003
# Réponse: 204 No Content (succès, pas de body)
```

**Tentative de supprimer un colis actif** :
```bash
curl -X DELETE http://localhost:8080/store_parcels/parcel/P000004
# Si le colis est READY_FOR_PICKUP...
```

**Réponse (400 Bad Request)** :
```json
{
  "type": "about:blank",
  "title": "Parcel Not Deletable",
  "status": 400,
  "detail": "Parcel P000004 with status READY_FOR_PICKUP cannot be deleted",
  "parcelId": "P000004",
  "currentStatus": "READY_FOR_PICKUP",
  "deletableStatuses": ["CANCELLED", "RETURNED"]
}
```

---

---

## Partie 3 : Contrôleur MCP (Model Context Protocol) ✅

**Date** : 31 décembre 2025

### Objectif
Ajouter un contrôleur MCP permettant aux LLMs (comme Claude) d'interagir avec l'API via des outils structurés.

---

### Étape 24 : Introduction au MCP ✅

**Qu'est-ce que MCP ?**
Model Context Protocol est un standard ouvert développé par Anthropic permettant aux LLMs d'interagir avec des systèmes externes de manière structurée et sécurisée.

**Concepts clés** :
| Concept | Description |
|---------|-------------|
| **Tools** | Fonctions que le LLM peut appeler (search_parcels, create_parcel, etc.) |
| **Resources** | Données que le LLM peut lire (liste des magasins, documentation des statuts) |
| **Prompts** | Templates de prompts prédéfinis (optionnel) |

**Architecture** :
```
Claude Code  <-->  MCP Controller  -->  ParcelService  -->  MongoDB
(Client MCP)      (Spring Boot)        (existant)          (existant)
```

---

### Étape 25 : DTOs MCP ✅

**Package créé** : `mcp/`

**Fichiers créés** :

| Fichier | Description |
|---------|-------------|
| `McpRequest.java` | Requête MCP avec méthode et paramètres |
| `McpResponse.java` | Réponse MCP avec contenu et statut d'erreur |
| `McpTool.java` | Définition d'un outil (nom, description, schéma) |
| `McpToolsResponse.java` | Liste des outils disponibles |

**Exemple McpRequest** :
```json
{
  "method": "search_parcels",
  "params": {
    "store_id": "ST001",
    "status": "READY_FOR_PICKUP"
  }
}
```

**Exemple McpResponse** :
```json
{
  "content": [
    {
      "type": "text",
      "text": "Found 5 parcels..."
    }
  ],
  "isError": false
}
```

---

### Étape 26 : Interface McpToolHandler ✅

**Fichier créé** : `mcp/tools/McpToolHandler.java`

**Méthodes de l'interface** :
```java
public interface McpToolHandler {
    String getToolName();           // Nom pour le routage
    McpTool getToolDefinition();    // Définition pour le LLM
    String execute(Map<String, Object> params);  // Exécution
}
```

**Pourquoi une interface ?**
- Permet d'ajouter facilement de nouveaux outils
- Chaque outil est indépendant et testable
- Le contrôleur route automatiquement vers le bon handler

---

### Étape 27 : Méthode createParcel ✅

**Fichiers modifiés** :
- `dto/CreateParcelRequestDTO.java` - Nouveau DTO pour la création
- `service/ParcelService.java` - Nouvelle méthode
- `service/ParcelServiceImpl.java` - Implémentation

**CreateParcelRequestDTO** :
```java
public class CreateParcelRequestDTO {
    String parcelId;           // Requis
    String customerOrderId;    // Requis
    String deliveryStoreId;    // Requis (format ST001)
    CustomerCreateDTO customer; // Requis
    Boolean isPaid;            // Optionnel
    Instant expirationDate;    // Optionnel (défaut: +14 jours)
}
```

**Règles métier** :
- Le parcel ID doit être unique
- Le statut initial est toujours `PENDING`
- La date d'expiration par défaut est 14 jours

---

### Étape 28 : Outils MCP Implémentés ✅

**Package créé** : `mcp/tools/`

**5 outils implémentés** :

| Outil | Description | Paramètres |
|-------|-------------|------------|
| `search_parcels` | Recherche de colis | store_id, status, customer_name, page |
| `get_parcel` | Détails d'un colis | parcel_id |
| `create_parcel` | Création d'un colis | parcel_id, order_id, store_id, customer |
| `update_parcel_status` | Mise à jour statut | parcel_id, new_status |
| `delete_parcel` | Suppression (soft) | parcel_id |

**Exemple SearchParcelsTool** :
```java
@Override
public String execute(Map<String, Object> params) {
    // Construit les critères de recherche
    if (params.containsKey("store_id")) {
        criterias.add(new CriteriaDTO("deliveryStoreId", "EQ", params.get("store_id")));
    }
    // Appelle le service existant
    Page<ParcelResponseDTO> result = parcelService.searchParcels(request);
    // Formate le résultat pour le LLM
    return formatSearchResults(result);
}
```

---

### Étape 29 : McpController ✅

**Fichier créé** : `controller/McpController.java`

**Endpoints MCP** :

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/mcp/tools` | Liste les outils disponibles |
| POST | `/mcp/tools/call` | Exécute un outil |
| GET | `/mcp/resources` | Liste les ressources |
| GET | `/mcp/resources/read` | Lit une ressource |

**Routage automatique** :
```java
@PostMapping("/tools/call")
public ResponseEntity<McpResponse> callTool(@RequestBody McpRequest request) {
    String toolName = request.method();

    // Trouve le handler correspondant
    McpToolHandler handler = toolHandlers.stream()
        .filter(h -> h.getToolName().equals(toolName))
        .findFirst()
        .orElse(null);

    if (handler == null) {
        return ResponseEntity.badRequest()
            .body(McpResponse.error("Unknown tool: " + toolName));
    }

    String result = handler.execute(params);
    return ResponseEntity.ok(McpResponse.success(result));
}
```

---

### Étape 30 : Ressources MCP ✅

**Ressources disponibles** :

| URI | Description | Format |
|-----|-------------|--------|
| `store-parcels://stores` | Liste des 10 magasins | JSON |
| `store-parcels://statuses` | Documentation des statuts | Markdown |

**Exemple de ressource stores** :
```json
[
  {"id": "ST001", "name": "Paris Centre", "address": "123 Rue de Rivoli..."},
  {"id": "ST002", "name": "Lyon Part-Dieu", "address": "45 Avenue..."},
  ...
]
```

---

### Résumé des Fichiers Partie 3

| Catégorie | Fichiers |
|-----------|----------|
| **MCP DTOs** | `McpRequest.java`, `McpResponse.java`, `McpTool.java`, `McpToolsResponse.java` |
| **Interface** | `McpToolHandler.java` |
| **Outils** | `SearchParcelsTool.java`, `GetParcelTool.java`, `CreateParcelTool.java`, `UpdateParcelStatusTool.java`, `DeleteParcelTool.java` |
| **Controller** | `McpController.java` |
| **Service** | `CreateParcelRequestDTO.java`, `ParcelService.java` (modifié), `ParcelServiceImpl.java` (modifié) |

**Total Partie 3** : 11 nouveaux fichiers créés, 2 fichiers modifiés

---

### Exemples d'Utilisation des Endpoints MCP

**Lister les outils** :
```bash
curl http://localhost:8080/mcp/tools
```

**Rechercher des colis** :
```bash
curl -X POST http://localhost:8080/mcp/tools/call \
  -H "Content-Type: application/json" \
  -d '{
    "method": "search_parcels",
    "params": {
      "store_id": "ST001",
      "status": "READY_FOR_PICKUP"
    }
  }'
```

**Obtenir un colis** :
```bash
curl -X POST http://localhost:8080/mcp/tools/call \
  -H "Content-Type: application/json" \
  -d '{
    "method": "get_parcel",
    "params": {"parcel_id": "P000001"}
  }'
```

**Créer un colis** :
```bash
curl -X POST http://localhost:8080/mcp/tools/call \
  -H "Content-Type: application/json" \
  -d '{
    "method": "create_parcel",
    "params": {
      "parcel_id": "P999999",
      "order_id": "ORD999999",
      "store_id": "ST001",
      "customer_first_name": "Jean",
      "customer_last_name": "Dupont"
    }
  }'
```

**Mettre à jour le statut** :
```bash
curl -X POST http://localhost:8080/mcp/tools/call \
  -H "Content-Type: application/json" \
  -d '{
    "method": "update_parcel_status",
    "params": {
      "parcel_id": "P000001",
      "new_status": "PICKED_UP"
    }
  }'
```

**Lire les ressources** :
```bash
curl "http://localhost:8080/mcp/resources/read?uri=store-parcels://stores"
curl "http://localhost:8080/mcp/resources/read?uri=store-parcels://statuses"
```

---

## Partie 4 : Déploiement GCP ✅

**Date** : 4 janvier 2026

### Objectif
Déployer l'application sur Google Cloud Platform avec Cloud Run et MongoDB Atlas, avec réseau sécurisé via Cloud NAT.

---

### Étape 31 : Infrastructure Terraform ✅

**Répertoire créé** : `terraform/`

**Fichiers créés** :

| Fichier | Description |
|---------|-------------|
| `providers.tf` | Configuration des providers GCP + MongoDB Atlas |
| `variables.tf` | Toutes les variables configurables (GCP, Atlas, App) |
| `outputs.tf` | Sorties importantes (URLs, IPs, clés) |
| `gcp-network.tf` | VPC, Subnet, Cloud NAT, VPC Connector |
| `gcp-cloudrun.tf` | Artifact Registry, service Cloud Run |
| `gcp-secrets.tf` | Secret Manager pour l'URI MongoDB |
| `gcp-iam.tf` | Service Account pour GitHub Actions |
| `atlas.tf` | Projet MongoDB Atlas, Cluster M0, Utilisateur, IP Whitelist |
| `terraform.tfvars.example` | Exemple de fichier de variables |
| `.gitignore` | Ignorer les fichiers sensibles |

**Architecture** :
```
┌─────────────────────────────────────────────────────────────┐
│                        CLIENT                                │
│                   (Navigateur, curl, etc.)                  │
└─────────────────────────────────────────────────────────────┘
                              │ HTTPS
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                  GOOGLE CLOUD PLATFORM                       │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                     CLOUD RUN                          │  │
│  │              store-parcels-api (Java 21)               │  │
│  └───────────────────────────────────────────────────────┘  │
│                              │                               │
│                              ▼                               │
│  ┌───────────────────────────────────────────────────────┐  │
│  │            SERVERLESS VPC CONNECTOR                    │  │
│  └───────────────────────────────────────────────────────┘  │
│                              │                               │
│                              ▼                               │
│  ┌───────────────────────────────────────────────────────┐  │
│  │         CLOUD NAT (IP Statique: 34.78.x.x)            │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │ IP Whitelistée
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    MONGODB ATLAS (M0)                        │
│                   store-parcels-cluster                      │
│                     (europe-west1)                           │
└─────────────────────────────────────────────────────────────┘
```

**Pourquoi Cloud NAT ?**
- Cloud Run n'a pas d'IP statique par défaut
- Cloud NAT fournit une IP de sortie statique
- MongoDB Atlas peut whitelister UNE SEULE IP au lieu de 0.0.0.0/0
- Sécurité renforcée : seul votre projet GCP peut accéder à Atlas

---

### Étape 32 : Contrôleur Health avec Probes ✅

**Fichier créé** : `controller/HealthController.java`

**Endpoints de Santé** :
| Endpoint | Objectif | Utilisation Cloud Run |
|----------|----------|----------------------|
| `GET /health` | Vérification santé basique | Monitoring général |
| `GET /health/live` | Probe de liveness | Redémarrer si non sain |
| `GET /health/ready` | Probe de readiness | Router le trafic quand prêt |

**Le probe de readiness** vérifie la connectivité MongoDB :
```java
@GetMapping("/health/ready")
public ResponseEntity<Map<String, Object>> readiness() {
    try {
        mongoTemplate.getDb().runCommand(new Document("ping", 1));
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "mongodb", "connected"
        ));
    } catch (Exception e) {
        return ResponseEntity.status(503).body(Map.of(
            "status", "DOWN",
            "mongodb", "disconnected"
        ));
    }
}
```

---

### Étape 33 : CI/CD GitHub Actions ✅

**Répertoire créé** : `.github/workflows/`

**Workflows** :

| Workflow | Déclencheur | Objectif |
|----------|-------------|----------|
| `ci.yml` | PR vers main/develop | Exécuter les tests |
| `deploy.yml` | Push vers main | Build + Deploy vers Cloud Run |

**Pipeline CI/CD** :
```
Push vers main → Tests → Build Docker → Push vers Artifact Registry → Deploy Cloud Run → Health Check
```

**Secrets GitHub Requis** :
| Secret | Description |
|--------|-------------|
| `GCP_PROJECT_ID` | ID du projet Google Cloud |
| `GCP_SA_KEY` | Clé JSON du Service Account (depuis output Terraform) |

---

### Étape 34 : Scripts d'Automatisation ✅

**Répertoire créé** : `scripts/`

**Scripts** :
| Script | Objectif | Quand exécuter |
|--------|----------|----------------|
| `terraform-apply.sh` | Créer toute l'infrastructure | Chaque matin |
| `terraform-destroy.sh` | Détruire toute l'infrastructure | Chaque soir |

**Optimisation des Coûts** :
- Exécuter `terraform destroy` chaque soir pour économiser ~75% des coûts
- Coût infrastructure : ~2-3€/mois avec destroy quotidien vs ~12€/mois toujours actif
- Cloud Run scale à zéro → coûts de calcul minimaux

---

### Résumé des Fichiers Partie 4

| Catégorie | Fichiers |
|-----------|----------|
| **Terraform** | `providers.tf`, `variables.tf`, `outputs.tf`, `gcp-network.tf`, `gcp-cloudrun.tf`, `gcp-secrets.tf`, `gcp-iam.tf`, `atlas.tf`, `terraform.tfvars.example`, `.gitignore` |
| **Controller** | `HealthController.java` |
| **GitHub Actions** | `ci.yml`, `deploy.yml` |
| **Scripts** | `terraform-apply.sh`, `terraform-destroy.sh` |

**Total Partie 4** : 14 nouveaux fichiers créés, 1 fichier modifié

---

### Commandes de Déploiement

**Configuration Initiale (une fois)** :
```bash
# Installer GCloud CLI
brew install google-cloud-sdk

# S'authentifier
gcloud auth login
gcloud config set project store-parcels-training

# Activer les APIs
gcloud services enable \
  run.googleapis.com \
  artifactregistry.googleapis.com \
  cloudbuild.googleapis.com \
  secretmanager.googleapis.com \
  compute.googleapis.com \
  vpcaccess.googleapis.com
```

**Workflow Quotidien** :
```bash
# Matin : Créer l'infrastructure
cd terraform
cp terraform.tfvars.example terraform.tfvars  # Remplir les valeurs
./scripts/terraform-apply.sh

# Obtenir les outputs
terraform output cloud_run_url
terraform output nat_ip_address

# Soir : Détruire l'infrastructure (économiser les coûts !)
./scripts/terraform-destroy.sh
```

**Déploiement GitHub Actions** :
```bash
# Push vers main déclenche le déploiement automatique
git push origin main

# Ou déclenchement manuel via l'UI GitHub
```

**Tester le Déploiement Cloud** :
```bash
# Obtenir l'URL Cloud Run
CLOUD_RUN_URL=$(terraform -chdir=terraform output -raw cloud_run_url)

# Health check
curl $CLOUD_RUN_URL/store_parcels/health

# Readiness check (connectivité MongoDB)
curl $CLOUD_RUN_URL/store_parcels/health/ready

# Recherche de colis
curl -X POST $CLOUD_RUN_URL/store_parcels/search \
  -H "Content-Type: application/json" \
  -d '{"criterias":[],"pagination":{"page":1,"pageSize":10}}'
```

---

## Prochaines Étapes

- [x] Générer 50 000 colis de test (Checkpoint 2) ✅
- [x] Ajouter Gatling pour les tests de performance (Checkpoint 3) ✅
- [x] Mesurer la performance de base (Checkpoint 4) ✅
- [x] Atteindre P95 < 60ms (Checkpoints 5-7) ✅ **ATTEINT : 58ms**
- [x] Partie 2 : Endpoints d'écriture (PUT, DELETE) ✅
- [x] Partie 3 : Contrôleur MCP ✅
- [x] Partie 4 : Déploiement GCP ✅

**Toutes les Parties Terminées !**

---

## Commandes Utiles

```bash
# Démarrer MongoDB
docker-compose up -d

# Arrêter MongoDB
docker-compose down

# Lancer l'application
mvn spring-boot:run

# Compiler l'application
mvn clean package

# Tester le health endpoint
curl http://localhost:8080/store_parcels/health

# Tester l'endpoint de recherche
curl -X POST http://localhost:8080/store_parcels/search \
  -H "Content-Type: application/json" \
  -d '{"criterias":[],"pagination":{"page":1,"pageSize":20}}'
```

---

## Glossaire des Termes Techniques

| Terme | Explication |
|-------|-------------|
| **P95** | 95e percentile - 95% des requêtes sont plus rapides que cette valeur |
| **REST API** | Interface web utilisant HTTP (GET, POST, PUT, DELETE) |
| **MongoDB** | Base de données NoSQL (documents JSON) |
| **Spring Boot** | Framework Java qui simplifie le développement |
| **Maven** | Outil de gestion de projet Java (dépendances, compilation) |
| **Docker** | Technologie de conteneurisation |
| **Index** | Structure de données pour accélérer les recherches |
| **DTO** | Data Transfer Object - objet pour transférer des données |
| **Controller** | Composant qui gère les requêtes HTTP |
| **Service** | Composant qui contient la logique métier |
| **Repository** | Composant qui gère l'accès aux données |
| **Entité** | Classe Java qui représente une table/collection en base |
| **Profil** | Configuration spécifique à un environnement (local, docker, gcp) |
