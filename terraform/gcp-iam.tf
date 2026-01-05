# ============================================
# GCP IAM Configuration
# ============================================
# Service Account for GitHub Actions deployment

# ============================================
# Service Account for GitHub Actions
# ============================================
# Used for deploying Docker images and Cloud Run services

resource "google_service_account" "github_actions" {
  account_id   = "github-actions"
  display_name = "GitHub Actions Deployment"
  description  = "Service account for GitHub Actions CI/CD pipeline"
}

# ============================================
# IAM Roles for GitHub Actions
# ============================================
# Minimal permissions for deploying to Cloud Run

locals {
  github_actions_roles = [
    "roles/run.admin",               # Deploy to Cloud Run
    "roles/artifactregistry.writer", # Push Docker images
    "roles/iam.serviceAccountUser",  # Use the Cloud Run service account
  ]
}

resource "google_project_iam_member" "github_actions" {
  for_each = toset(local.github_actions_roles)
  project  = var.gcp_project_id
  role     = each.value
  member   = "serviceAccount:${google_service_account.github_actions.email}"
}

# ============================================
# Service Account Key
# ============================================
# JSON key to be stored in GitHub Secrets

resource "google_service_account_key" "github_actions" {
  service_account_id = google_service_account.github_actions.name
}
