# ============================================
# GCP Secret Manager Configuration
# ============================================
# Stores MongoDB connection string securely

# Get current project data
data "google_project" "current" {}

# ============================================
# MongoDB URI Secret
# ============================================

resource "google_secret_manager_secret" "mongodb_uri" {
  secret_id = "${var.app_name}-mongodb-uri"

  replication {
    auto {}
  }

  labels = {
    app = var.app_name
    env = "training"
  }
}

# ============================================
# MongoDB URI Secret Version
# ============================================
# Constructs the connection string from Atlas cluster info

resource "google_secret_manager_secret_version" "mongodb_uri" {
  secret = google_secret_manager_secret.mongodb_uri.id

  # Construct MongoDB Atlas connection string
  secret_data = "mongodb+srv://storeparcels_app:${var.mongodb_password}@${mongodbatlas_cluster.store_parcels.connection_strings[0].standard_srv != "" ? replace(mongodbatlas_cluster.store_parcels.connection_strings[0].standard_srv, "mongodb+srv://", "") : "${mongodbatlas_cluster.store_parcels.name}.mongodb.net"}/store_parcels_db?retryWrites=true&w=majority"

  depends_on = [mongodbatlas_cluster.store_parcels]
}

# ============================================
# IAM - Cloud Run Access to Secret
# ============================================
# Allow Cloud Run default service account to access the secret

resource "google_secret_manager_secret_iam_member" "cloudrun_access" {
  secret_id = google_secret_manager_secret.mongodb_uri.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${data.google_project.current.number}-compute@developer.gserviceaccount.com"
}
