# Troubleshooting Journey: From Local to Cloud

## The Story of Deploying StoreParcelAPI to Production

**Date**: January 5, 2026
**Duration**: ~3 hours
**Final Result**: Success! API live at `https://store-parcels-api-1065577871620.europe-west1.run.app`

---

## Table of Contents

1. [Tools & Software Explained](#tools--software-explained)
2. [Issue #1: Not a Git Repository](#issue-1-not-a-git-repository)
3. [Issue #2: Maven Wrapper Broken](#issue-2-maven-wrapper-broken)
4. [Issue #3: GCP Authentication Failed](#issue-3-gcp-authentication-failed)
5. [Issue #4: Artifact Registry Not Found](#issue-4-artifact-registry-not-found)
6. [Issue #5: VPC Connector Does Not Exist](#issue-5-vpc-connector-does-not-exist)
7. [Issue #6: MongoDB Authentication Failed](#issue-6-mongodb-authentication-failed)
8. [Lessons Learned](#lessons-learned)

---

## Tools & Software Explained

### Development Tools

| Tool | What is it? | Why we use it |
|------|-------------|---------------|
| **Git** | Version control system | Tracks changes to code, enables collaboration |
| **GitHub** | Cloud hosting for Git repositories | Stores our code online, enables CI/CD |
| **Maven** | Java build tool | Compiles code, manages dependencies, runs tests |
| **Maven Wrapper (mvnw)** | Portable Maven script | Ensures everyone uses the same Maven version |
| **Docker** | Container platform | Packages our app with all dependencies |

### Cloud Services

| Service | What is it? | Why we use it |
|---------|-------------|---------------|
| **Google Cloud Platform (GCP)** | Cloud computing platform | Hosts our application |
| **Cloud Run** | Serverless container service | Runs our Docker container, scales automatically |
| **Artifact Registry** | Container image storage | Stores our Docker images |
| **MongoDB Atlas** | Cloud database service | Hosts our MongoDB database |

### CLI Tools

| Tool | What is it? | Why we use it |
|------|-------------|---------------|
| **gcloud** | Google Cloud CLI | Manages GCP resources from terminal |
| **gh** | GitHub CLI | Manages GitHub repos, secrets, actions from terminal |
| **curl** | HTTP client | Tests API endpoints |
| **Homebrew** | macOS package manager | Installs software easily |

### CI/CD

| Tool | What is it? | Why we use it |
|------|-------------|---------------|
| **GitHub Actions** | Automation platform | Automatically tests and deploys code |
| **Workflow** | YAML configuration file | Defines what GitHub Actions should do |
| **Secrets** | Encrypted variables | Stores sensitive data (passwords, keys) |

---

## Issue #1: Not a Git Repository

### The Error
```
fatal: not a git repository (or any of the parent directories): .git
```

### When it happened
When trying to run `git add` and `git commit` commands.

### Why it happened
The user was in the wrong directory (`/Users/redouane/Desktop/homework`) instead of the project directory (`/Users/redouane/Desktop/homework/storeparcelapi`).

**Explanation:**
- Git repositories have a hidden `.git` folder that contains all version history
- Commands like `git add`, `git commit` only work inside a git repository
- The parent `homework` folder was not a git repo

### The Solution
Navigate to the correct directory:
```bash
cd /Users/redouane/Desktop/homework/storeparcelapi
git status  # Now works!
```

### Lesson Learned
Always check your current directory with `pwd` before running git commands.

---

## Issue #2: Maven Wrapper Broken

### The Error
```
-Dmaven.multiModuleProjectDirectory system property is not set.
Error: Process completed with exit code 1.
```

### When it happened
During GitHub Actions CI workflow when running `./mvnw test -B`.

### Why it happened
The `mvnw` (Maven Wrapper) script was incomplete. Someone had created a simplified version that was missing a critical line:

**Broken version (27 lines):**
```bash
#!/bin/sh
# Simple script that was missing important settings
exec java -classpath "$MAVEN_WRAPPER_JAR" \
    org.apache.maven.wrapper.MavenWrapperMain "$@"
```

**Working version (295 lines):**
```bash
#!/bin/sh
# Full Apache Maven Wrapper with all necessary settings
# Including the critical line:
exec "$JAVACMD" \
    -Dmaven.multiModuleProjectDirectory="$MAVEN_PROJECTBASEDIR" \
    # ... many more settings
```

**Explanation:**
- `maven.multiModuleProjectDirectory` tells Maven where the project root is
- Without it, Maven 3.3.1+ refuses to run
- The Maven Wrapper script must set this property

### The Solution
Regenerate the proper Maven Wrapper:
```bash
mvn wrapper:wrapper -Dmaven=3.9.6
```

This created:
- `mvnw` - Unix shell script (295 lines, 11KB)
- `mvnw.cmd` - Windows batch script
- `.mvn/wrapper/maven-wrapper.properties` - Configuration

### Lesson Learned
Never manually edit Maven Wrapper files. Always regenerate them with `mvn wrapper:wrapper`.

---

## Issue #3: GCP Authentication Failed

### The Error
```
FAILED: Authenticate to Google Cloud
```

### When it happened
During GitHub Actions deploy workflow.

### Why it happened
GitHub Actions was trying to authenticate to Google Cloud, but no credentials were configured.

**Explanation:**
Think of it like this:
```
GitHub Actions: "Hey GCP, deploy this app!"
Google Cloud: "Who are you? Show me your ID!"
GitHub Actions: "I don't have one..." ❌
```

GitHub Secrets were empty:
- `GCP_PROJECT_ID` - Not set
- `GCP_SA_KEY` - Not set

### The Solution

**Step 1: Create a Service Account (robot user)**
```bash
gcloud iam service-accounts create github-actions \
  --display-name="GitHub Actions"
```

**Step 2: Give it permissions**
```bash
gcloud projects add-iam-policy-binding store-parcels-app \
  --member="serviceAccount:github-actions@store-parcels-app.iam.gserviceaccount.com" \
  --role="roles/run.admin"
```

**Step 3: Create a JSON key**
```bash
gcloud iam service-accounts keys create ~/gcp-sa-key.json \
  --iam-account=github-actions@store-parcels-app.iam.gserviceaccount.com
```

**Step 4: Add secrets to GitHub**
```bash
gh secret set GCP_PROJECT_ID --body "store-parcels-app"
gh secret set GCP_SA_KEY < ~/gcp-sa-key.json
```

### Lesson Learned
CI/CD pipelines need credentials to access cloud services. Always set up service accounts with minimal necessary permissions.

---

## Issue #4: Artifact Registry Not Found

### The Error
```
name unknown: Repository "store-parcels-repo" not found
```

### When it happened
During Docker push step in GitHub Actions.

### Why it happened
We were trying to push a Docker image to a repository that didn't exist yet.

**Explanation:**
- Artifact Registry is like a warehouse for Docker images
- Before you can store images, you need to create the warehouse (repository)
- The GitHub Actions workflow assumed the repository already existed

### The Solution
Create the Artifact Registry repository:
```bash
gcloud artifacts repositories create store-parcels-repo \
  --repository-format=docker \
  --location=europe-west1 \
  --description="Docker repository for Store Parcels API"
```

### Lesson Learned
Infrastructure must exist before CI/CD can use it. Either create resources manually first, or use Infrastructure as Code (Terraform) to create them automatically.

---

## Issue #5: VPC Connector Does Not Exist

### The Error
```
VPC connector projects/store-parcels-app/locations/europe-west1/connectors/store-parcels-connector
does not exist, or Cloud Run does not have permission to use it.
```

### When it happened
During Cloud Run deployment.

### Why it happened
The deployment workflow was configured to use a VPC connector that we never created.

**What is a VPC Connector?**
- VPC = Virtual Private Cloud (private network)
- VPC Connector allows Cloud Run to access resources in your private network
- Originally planned for secure MongoDB Atlas connection with static IP
- We didn't set this up because it requires more configuration

**The workflow had:**
```yaml
flags: |
  --vpc-connector=store-parcels-connector  # This doesn't exist!
  --vpc-egress=all-traffic
```

### The Solution

**Option A (Quick fix - what we did):**
Remove the VPC connector requirement:
```yaml
flags: |
  --allow-unauthenticated
  --min-instances=0
  --max-instances=1
```

**Option B (Better for production):**
Create the VPC connector with Terraform (more complex, not needed for learning).

**Additional Problem:**
After removing VPC connector from workflow, old Cloud Run revision still had it configured. Had to delete the service:
```bash
gcloud run services delete store-parcels-api --region=europe-west1
```

Then re-deploy fresh.

### Lesson Learned
When modifying Cloud Run settings, sometimes you need to delete and recreate the service if old configurations persist.

---

## Issue #6: MongoDB Authentication Failed

### The Error
```json
{
  "error": "Exception authenticating MongoCredential{mechanism=SCRAM-SHA-1, userName='store-parcels-user'...}",
  "status": "DOWN",
  "mongodb": "disconnected"
}
```

### When it happened
After deploying to Cloud Run and testing the `/health/ready` endpoint.

### Why it happened
Multiple authentication issues:

1. **Wrong username** - We guessed `store-parcels-user` but it might have been different
2. **Wrong password** - Password was entered incorrectly
3. **Password not updated** - Old password still active

**Explanation:**
MongoDB Atlas authentication requires:
- Exact username (case-sensitive)
- Exact password (special characters must be URL-encoded)
- User must exist in Database Access
- IP must be whitelisted in Network Access

### The Solution

**Step 1: Find the correct username**
Go to MongoDB Atlas → Database Access → Check exact username

**Step 2: Reset the password**
Click Edit → Edit Password → Set new simple password: `Kaka1001.`

**Step 3: Update Cloud Run with correct credentials**
```bash
gcloud run services update store-parcels-api \
  --region=europe-west1 \
  --set-env-vars="SPRING_DATA_MONGODB_URI=mongodb+srv://store-parcels-user:Kaka1001.@store-parcels-cluster.iuppnkd.mongodb.net/store_parcels?retryWrites=true&w=majority"
```

**Step 4: Test connection**
```bash
curl https://store-parcels-api-xxx.run.app/store_parcels/health/ready
# Response: {"status":"UP","mongodb":"connected"}
```

### Lesson Learned
- Always double-check credentials from the source (MongoDB Atlas dashboard)
- Use simple passwords during development (no special characters)
- Test database connection with health endpoints before testing full functionality

---

## Summary: The Complete Journey

```
START
  │
  ├── Issue #1: Wrong directory
  │   └── Fix: cd to correct folder
  │
  ├── Issue #2: Broken Maven Wrapper
  │   └── Fix: mvn wrapper:wrapper
  │
  ├── Issue #3: No GCP credentials
  │   └── Fix: Create service account + GitHub secrets
  │
  ├── Issue #4: No Artifact Registry
  │   └── Fix: gcloud artifacts repositories create
  │
  ├── Issue #5: Missing VPC Connector
  │   └── Fix: Remove VPC requirement + delete old service
  │
  ├── Issue #6: MongoDB auth failed
  │   └── Fix: Verify credentials + reset password
  │
  ▼
SUCCESS! 🎉
API live at: https://store-parcels-api-1065577871620.europe-west1.run.app
```

---

## Lessons Learned

### 1. Read Error Messages Carefully
Every error message tells you exactly what's wrong. Don't panic - read it slowly.

### 2. Check Prerequisites
Before deploying, ensure all infrastructure exists:
- [ ] GCP project created
- [ ] APIs enabled
- [ ] Service account created
- [ ] Secrets configured
- [ ] Repositories created
- [ ] Database set up

### 3. Test Incrementally
Don't try to fix everything at once:
1. First, make sure code builds locally
2. Then, make sure tests pass
3. Then, make sure Docker builds
4. Then, deploy to cloud
5. Finally, connect to database

### 4. Use Health Endpoints
Always add `/health` and `/health/ready` endpoints. They tell you:
- Is the app running? (`/health`)
- Is the database connected? (`/health/ready`)

### 5. Keep Credentials Simple During Development
- Use simple passwords (no special characters)
- Use `0.0.0.0/0` for IP whitelist (but NOT in production!)
- Add proper security later

### 6. Infrastructure as Code is Worth It
For production, use Terraform to:
- Create all resources automatically
- Ensure consistency between environments
- Document your infrastructure

---

## Tools Quick Reference

```bash
# Git
git status                    # Check current state
git add .                     # Stage all changes
git commit -m "message"       # Commit changes
git push origin main          # Push to GitHub

# GCloud
gcloud auth login             # Login to GCP
gcloud config set project X   # Set default project
gcloud run services list      # List Cloud Run services
gcloud run services delete X  # Delete a service

# GitHub CLI
gh auth login                 # Login to GitHub
gh secret set NAME            # Set a secret
gh secret list                # List secrets
gh run list                   # List workflow runs
gh run view ID                # View run details

# Testing
curl URL/health               # Test health
curl -X POST URL/endpoint     # Test POST endpoint
```

---

**Total Issues Encountered:** 6
**Total Time to Resolve:** ~3 hours
**Final Status:** ✅ SUCCESS - API deployed and running!

*Remember: Every expert was once a beginner who refused to give up.* 🚀
