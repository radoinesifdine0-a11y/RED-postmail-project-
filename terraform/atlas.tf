# ============================================
# MongoDB Atlas Configuration
# ============================================
# Project, Cluster (M0 Free Tier), Database User, IP Whitelist

# ============================================
# Atlas Project
# ============================================

resource "mongodbatlas_project" "store_parcels" {
  name   = "${var.app_name}-training"
  org_id = var.atlas_org_id
}

# ============================================
# Atlas Cluster - M0 Free Tier
# ============================================
# Free tier with 512MB storage, shared cluster

resource "mongodbatlas_cluster" "store_parcels" {
  project_id = mongodbatlas_project.store_parcels.id
  name       = "${var.app_name}-cluster"

  # M0 Free Tier Configuration
  provider_name               = "TENANT"
  backing_provider_name       = "GCP"
  provider_region_name        = "WESTERN_EUROPE"  # Close to europe-west1
  provider_instance_size_name = "M0"

  # M0 specific settings
  # Note: M0 clusters have limited configuration options
}

# ============================================
# Database User
# ============================================

resource "mongodbatlas_database_user" "app_user" {
  project_id         = mongodbatlas_project.store_parcels.id
  username           = "storeparcels_app"
  password           = var.mongodb_password
  auth_database_name = "admin"

  roles {
    role_name     = "readWrite"
    database_name = "store_parcels_db"
  }

  # Allow access from anywhere initially (will be restricted by IP whitelist)
  scopes {
    name = mongodbatlas_cluster.store_parcels.name
    type = "CLUSTER"
  }
}

# ============================================
# IP Whitelist - Cloud NAT Static IP
# ============================================
# Only allow connections from our GCP Cloud NAT IP

resource "mongodbatlas_project_ip_access_list" "gcp_nat" {
  project_id = mongodbatlas_project.store_parcels.id
  ip_address = google_compute_address.nat_ip.address
  comment    = "GCP Cloud NAT - Store Parcels Training (${var.gcp_region})"

  depends_on = [google_compute_address.nat_ip]
}

# ============================================
# Local Development Access (optional)
# ============================================
# Uncomment to allow access from your local machine during development
# WARNING: Do not use in production!

# resource "mongodbatlas_project_ip_access_list" "local_dev" {
#   project_id = mongodbatlas_project.store_parcels.id
#   ip_address = "YOUR_LOCAL_IP"
#   comment    = "Local Development - TEMPORARY"
# }
