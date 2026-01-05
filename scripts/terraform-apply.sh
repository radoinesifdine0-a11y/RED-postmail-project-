#!/bin/bash
# ============================================
# Terraform Apply Script
# ============================================
# Creates the entire GCP + Atlas infrastructure
# Run this every morning before starting work

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TERRAFORM_DIR="${SCRIPT_DIR}/../terraform"
LOG_FILE="${SCRIPT_DIR}/terraform-apply-$(date +%Y%m%d-%H%M%S).log"

echo -e "${GREEN}=== Terraform Apply - $(date) ===${NC}"
echo "Log file: ${LOG_FILE}"
echo ""

# Check if terraform.tfvars exists
if [ ! -f "${TERRAFORM_DIR}/terraform.tfvars" ]; then
    echo -e "${RED}ERROR: terraform.tfvars not found!${NC}"
    echo "Please copy terraform.tfvars.example to terraform.tfvars and fill in your values."
    exit 1
fi

cd "${TERRAFORM_DIR}"

# ============================================
# Initialize Terraform
# ============================================
echo -e "${YELLOW}Step 1: Initializing Terraform...${NC}"
terraform init -upgrade 2>&1 | tee -a "${LOG_FILE}"
echo ""

# ============================================
# Plan
# ============================================
echo -e "${YELLOW}Step 2: Creating execution plan...${NC}"
terraform plan -out=tfplan 2>&1 | tee -a "${LOG_FILE}"
echo ""

# ============================================
# Apply
# ============================================
echo -e "${YELLOW}Step 3: Applying infrastructure changes...${NC}"
terraform apply tfplan 2>&1 | tee -a "${LOG_FILE}"
echo ""

# Clean up plan file
rm -f tfplan

# ============================================
# Wait for Atlas M0 Cluster
# ============================================
echo -e "${YELLOW}Step 4: Waiting for MongoDB Atlas cluster to be ready...${NC}"
echo "Atlas M0 clusters can take 3-5 minutes to provision..."
sleep 60

# ============================================
# Display Outputs
# ============================================
echo -e "${GREEN}=== Infrastructure Created Successfully ===${NC}"
echo ""
terraform output 2>&1 | tee -a "${LOG_FILE}"
echo ""

# ============================================
# Extract key values
# ============================================
CLOUD_RUN_URL=$(terraform output -raw cloud_run_url 2>/dev/null || echo "Not available yet")
NAT_IP=$(terraform output -raw nat_ip_address 2>/dev/null || echo "Not available")

echo -e "${GREEN}=== Quick Reference ===${NC}"
echo "Cloud Run URL: ${CLOUD_RUN_URL}"
echo "NAT IP (Atlas whitelist): ${NAT_IP}"
echo ""

# ============================================
# Health Check
# ============================================
if [ "${CLOUD_RUN_URL}" != "Not available yet" ]; then
    echo -e "${YELLOW}Step 5: Running health check...${NC}"
    sleep 10
    curl -f "${CLOUD_RUN_URL}/store_parcels/health" && echo -e "\n${GREEN}Health check passed!${NC}" || echo -e "\n${YELLOW}Health check failed (this is normal on first deploy - push code via GitHub Actions)${NC}"
fi

echo ""
echo -e "${GREEN}=== Terraform Apply Complete - $(date) ===${NC}"
echo ""
echo "Next steps:"
echo "1. Note the NAT IP above - it's automatically whitelisted in Atlas"
echo "2. Push code to main branch to trigger deployment via GitHub Actions"
echo "3. Run 'terraform-destroy.sh' tonight to save costs!"
