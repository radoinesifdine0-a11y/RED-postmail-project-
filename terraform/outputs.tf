# ============================================
# Terraform Outputs
# ============================================
# Important values exported after terraform apply

# ============================================
# Cloud Run Outputs
# ============================================

output "cloud_run_url" {
  description = "Cloud Run Service URL"
  value       = google_cloud_run_v2_service.store_parcels_api.uri
}

# ============================================
# Network Outputs
# ============================================

output "nat_ip_address" {
  description = "Cloud NAT Static IP (whitelisted in Atlas)"
  value       = google_compute_address.nat_ip.address
}

output "vpc_connector" {
  description = "VPC Connector Name"
  value       = google_vpc_access_connector.store_parcels_connector.name
}

# ============================================
# Artifact Registry Outputs
# ============================================

output "artifact_registry" {
  description = "Artifact Registry URL for Docker images"
  value       = "${var.gcp_region}-docker.pkg.dev/${var.gcp_project_id}/${google_artifact_registry_repository.store_parcels.repository_id}"
}

# ============================================
# MongoDB Atlas Outputs
# ============================================

output "atlas_cluster_name" {
  description = "MongoDB Atlas Cluster Name"
  value       = mongodbatlas_cluster.store_parcels.name
}

output "atlas_connection_string" {
  description = "MongoDB Atlas Connection String (without password)"
  value       = "mongodb+srv://storeparcels_app:****@${replace(mongodbatlas_cluster.store_parcels.connection_strings[0].standard_srv, "mongodb+srv://", "")}"
  sensitive   = true
}

# ============================================
# GitHub Actions Outputs
# ============================================

output "github_actions_service_account" {
  description = "Service Account email for GitHub Actions"
  value       = google_service_account.github_actions.email
}

output "github_actions_key" {
  description = "Service Account Key for GitHub Actions (base64 encoded)"
  value       = google_service_account_key.github_actions.private_key
  sensitive   = true
}
