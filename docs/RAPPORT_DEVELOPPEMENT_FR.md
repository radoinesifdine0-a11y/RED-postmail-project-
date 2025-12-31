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

## Prochaines Étapes

- [x] Générer 50 000 colis de test (Checkpoint 2) ✅
- [x] Ajouter Gatling pour les tests de performance (Checkpoint 3) ✅
- [x] Mesurer la performance de base (Checkpoint 4) ✅
- [x] Atteindre P95 < 60ms (Checkpoints 5-7) ✅ **ATTEINT : 58ms**
- [ ] Partie 2 : Endpoints d'écriture (POST, PUT, DELETE)
- [ ] Partie 3 : Contrôleur MCP
- [ ] Partie 4 : Déploiement GCP

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
