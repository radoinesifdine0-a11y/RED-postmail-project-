# ============================================
# GCP Network Configuration
# ============================================
# VPC, Subnet, Cloud NAT, and Serverless VPC Connector
# Enables Cloud Run to connect to MongoDB Atlas with static IP

# ============================================
# VPC Network
# ============================================

resource "google_compute_network" "store_parcels_vpc" {
  name                    = "${var.app_name}-vpc"
  auto_create_subnetworks = false
  description             = "VPC for Store Parcels API - enables static IP for MongoDB Atlas"
}

# ============================================
# Subnet
# ============================================

resource "google_compute_subnetwork" "store_parcels_subnet" {
  name          = "${var.app_name}-subnet"
  ip_cidr_range = "10.0.0.0/24"
  region        = var.gcp_region
  network       = google_compute_network.store_parcels_vpc.id
  description   = "Subnet for Store Parcels VPC"
}

# ============================================
# Static IP for Cloud NAT
# ============================================
# This IP will be whitelisted in MongoDB Atlas

resource "google_compute_address" "nat_ip" {
  name         = "${var.app_name}-nat-ip"
  region       = var.gcp_region
  address_type = "EXTERNAL"
  description  = "Static IP for Cloud NAT - whitelisted in MongoDB Atlas"
}

# ============================================
# Cloud Router
# ============================================
# Required for Cloud NAT

resource "google_compute_router" "store_parcels_router" {
  name    = "${var.app_name}-router"
  network = google_compute_network.store_parcels_vpc.id
  region  = var.gcp_region
}

# ============================================
# Cloud NAT
# ============================================
# Provides static IP for all outbound traffic from VPC

resource "google_compute_router_nat" "store_parcels_nat" {
  name   = "${var.app_name}-nat"
  router = google_compute_router.store_parcels_router.name
  region = var.gcp_region

  # Use static IP only
  nat_ip_allocate_option = "MANUAL_ONLY"
  nat_ips                = [google_compute_address.nat_ip.self_link]

  # NAT all traffic from all subnets
  source_subnetwork_ip_ranges_to_nat = "ALL_SUBNETWORKS_ALL_IP_RANGES"

  # Logging for debugging
  log_config {
    enable = true
    filter = "ERRORS_ONLY"
  }
}

# ============================================
# Serverless VPC Connector
# ============================================
# Bridges Cloud Run to VPC (and thus to Cloud NAT)

resource "google_vpc_access_connector" "store_parcels_connector" {
  name          = "${var.app_name}-connector"
  region        = var.gcp_region
  network       = google_compute_network.store_parcels_vpc.name
  ip_cidr_range = "10.8.0.0/28"

  # Minimum 2 instances required by GCP
  min_instances = 2
  max_instances = 2  # No scaling to minimize costs

  depends_on = [google_compute_network.store_parcels_vpc]
}
