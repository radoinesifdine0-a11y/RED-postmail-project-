# ============================================
# Terraform Variables
# ============================================
# All configurable parameters for the infrastructure

# ============================================
# GCP Variables
# ============================================

variable "gcp_project_id" {
  description = "GCP Project ID"
  type        = string
}

variable "gcp_region" {
  description = "GCP Region for deployment"
  type        = string
  default     = "europe-west1"
}

# ============================================
# MongoDB Atlas Variables
# ============================================

variable "atlas_public_key" {
  description = "MongoDB Atlas Public API Key"
  type        = string
  sensitive   = true
}

variable "atlas_private_key" {
  description = "MongoDB Atlas Private API Key"
  type        = string
  sensitive   = true
}

variable "atlas_org_id" {
  description = "MongoDB Atlas Organization ID"
  type        = string
}

variable "mongodb_password" {
  description = "MongoDB Database User Password"
  type        = string
  sensitive   = true
}

# ============================================
# Application Variables
# ============================================

variable "app_image_tag" {
  description = "Docker image tag for Cloud Run deployment"
  type        = string
  default     = "latest"
}

variable "app_name" {
  description = "Application name used for resource naming"
  type        = string
  default     = "store-parcels"
}
