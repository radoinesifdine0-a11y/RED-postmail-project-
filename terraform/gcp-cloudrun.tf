# ============================================
# GCP Cloud Run Configuration
# ============================================
# Artifact Registry and Cloud Run Service

# ============================================
# Artifact Registry
# ============================================
# Docker container registry for the application

resource "google_artifact_registry_repository" "store_parcels" {
  location      = var.gcp_region
  repository_id = "${var.app_name}-repo"
  format        = "DOCKER"
  description   = "Docker images for Store Parcels API"

  labels = {
    app = var.app_name
    env = "training"
  }
}

# ============================================
# Cloud Run Service
# ============================================

resource "google_cloud_run_v2_service" "store_parcels_api" {
  name     = "${var.app_name}-api"
  location = var.gcp_region

  # Wait for dependencies
  depends_on = [
    google_vpc_access_connector.store_parcels_connector,
    google_secret_manager_secret_version.mongodb_uri,
    mongodbatlas_project_ip_access_list.gcp_nat,
    google_artifact_registry_repository.store_parcels
  ]

  template {
    # ============================================
    # VPC Connector for Cloud NAT
    # ============================================
    vpc_access {
      connector = google_vpc_access_connector.store_parcels_connector.id
      egress    = "ALL_TRAFFIC"  # Route all egress through NAT
    }

    # ============================================
    # Container Configuration
    # ============================================
    containers {
      image = "${var.gcp_region}-docker.pkg.dev/${var.gcp_project_id}/${google_artifact_registry_repository.store_parcels.repository_id}/${var.app_name}-api:${var.app_image_tag}"

      # Environment Variables
      env {
        name  = "SPRING_PROFILES_ACTIVE"
        value = "gcp"
      }

      env {
        name  = "MONGODB_DATABASE"
        value = "store_parcels_db"
      }

      # MongoDB URI from Secret Manager
      env {
        name = "MONGODB_URI"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.mongodb_uri.secret_id
            version = "latest"
          }
        }
      }

      # ============================================
      # Resource Limits
      # ============================================
      resources {
        limits = {
          cpu    = "1"
          memory = "512Mi"
        }
        cpu_idle = true  # Allow CPU throttling when idle (cost saving)
      }

      # ============================================
      # Health Probes
      # ============================================

      # Startup Probe - wait for app to be ready
      startup_probe {
        http_get {
          path = "/store_parcels/health/ready"
        }
        initial_delay_seconds = 10
        period_seconds        = 3
        failure_threshold     = 10
        timeout_seconds       = 3
      }

      # Liveness Probe - is the app alive?
      liveness_probe {
        http_get {
          path = "/store_parcels/health/live"
        }
        period_seconds    = 30
        timeout_seconds   = 3
        failure_threshold = 3
      }
    }

    # ============================================
    # Scaling Configuration
    # ============================================
    scaling {
      min_instance_count = 0  # Scale to zero (cost saving)
      max_instance_count = 1  # Single instance (training)
    }

    # Service Account
    service_account = "${data.google_project.current.number}-compute@developer.gserviceaccount.com"

    # Timeout
    timeout = "60s"
  }

  # ============================================
  # Traffic Configuration
  # ============================================
  traffic {
    type    = "TRAFFIC_TARGET_ALLOCATION_TYPE_LATEST"
    percent = 100
  }

  labels = {
    app = var.app_name
    env = "training"
  }
}

# ============================================
# Allow Unauthenticated Access
# ============================================
# Make the API publicly accessible

resource "google_cloud_run_v2_service_iam_member" "public" {
  location = google_cloud_run_v2_service.store_parcels_api.location
  name     = google_cloud_run_v2_service.store_parcels_api.name
  role     = "roles/run.invoker"
  member   = "allUsers"
}
