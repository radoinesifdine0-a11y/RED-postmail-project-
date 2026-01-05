#!/bin/bash
# ============================================
# Terraform Destroy Script
# ============================================
# Destroys all GCP + Atlas infrastructure
# Run this every evening to minimize costs!

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TERRAFORM_DIR="${SCRIPT_DIR}/../terraform"
LOG_FILE="${SCRIPT_DIR}/terraform-destroy-$(date +%Y%m%d-%H%M%S).log"

echo -e "${RED}=== Terraform Destroy - $(date) ===${NC}"
echo "Log file: ${LOG_FILE}"
echo ""

cd "${TERRAFORM_DIR}"

# ============================================
# Check current state
# ============================================
echo -e "${YELLOW}Checking current infrastructure state...${NC}"
RESOURCE_COUNT=$(terraform state list 2>/dev/null | wc -l || echo "0")

if [ "${RESOURCE_COUNT}" -eq 0 ]; then
    echo -e "${GREEN}No infrastructure to destroy. State is empty.${NC}"
    exit 0
fi

echo "Found ${RESOURCE_COUNT} resources to destroy."
echo ""

# ============================================
# Backup state
# ============================================
if [ -f "terraform.tfstate" ]; then
    echo -e "${YELLOW}Backing up terraform state...${NC}"
    cp terraform.tfstate "terraform.tfstate.backup-$(date +%Y%m%d-%H%M%S)"
fi

# ============================================
# Confirmation (optional - comment out for cron)
# ============================================
if [ -t 0 ]; then
    # Running interactively
    echo -e "${RED}WARNING: This will destroy ALL infrastructure!${NC}"
    echo ""
    read -p "Are you sure you want to proceed? (yes/no): " CONFIRM
    if [ "${CONFIRM}" != "yes" ]; then
        echo "Aborted."
        exit 1
    fi
fi

# ============================================
# Destroy
# ============================================
echo -e "${YELLOW}Destroying infrastructure...${NC}"
echo ""

terraform destroy -auto-approve 2>&1 | tee -a "${LOG_FILE}"

# ============================================
# Verify destruction
# ============================================
echo ""
echo -e "${YELLOW}Verifying destruction...${NC}"
REMAINING=$(terraform state list 2>/dev/null | wc -l || echo "0")

if [ "${REMAINING}" -eq 0 ]; then
    echo -e "${GREEN}All resources destroyed successfully!${NC}"
else
    echo -e "${RED}WARNING: ${REMAINING} resources still remain in state.${NC}"
    terraform state list
fi

echo ""
echo -e "${GREEN}=== Terraform Destroy Complete - $(date) ===${NC}"
echo ""
echo "Cost savings enabled! Remember to run 'terraform-apply.sh' tomorrow morning."
