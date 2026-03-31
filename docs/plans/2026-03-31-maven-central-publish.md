# Maven Central Tag Publish Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a tag-triggered GitHub Actions release flow that publishes `xxl-job-core` to Maven Central with correct fork metadata, signing, and version validation.

**Architecture:** Keep publishing logic centered in the root Maven POM and a single GitHub Actions workflow. Release only on pushed tags matching `v*`, validate that the tag version equals the Maven project version, and deploy with the existing `release` profile so only `xxl-job-core` is uploaded.

**Tech Stack:** Maven, GitHub Actions, GPG, Java 8, Maven Central / Sonatype

---

### Task 1: Inspect current repository state for release-impacting changes

**Files:**
- Inspect: `pom.xml`
- Inspect: `.github/workflows/maven.yml`
- Inspect: `xxl-job-core/pom.xml`
- Inspect: `xxl-job-admin/pom.xml`
- Inspect: `xxl-job-executor-samples/pom.xml`

**Step 1: Check git status**

Run: `git status --short`
Expected: See any pre-existing local changes before editing release files.

**Step 2: Review current release metadata**

Run: `Get-Content pom.xml`
Expected: Confirm current `url`, `scm`, `developers`, `profiles`, and `distributionManagement`.

**Step 3: Review current workflow**

Run: `Get-Content .github/workflows/maven.yml`
Expected: Confirm current workflow only builds and does not publish.

**Step 4: Commit**

Do not commit in this task. This task exists to avoid overwriting unrelated local work.

### Task 2: Write a failing release validation check locally

**Files:**
- Create: `scripts/check-tag-version.ps1`
- Test: `scripts/check-tag-version.ps1`

**Step 1: Write the failing validation script**

Create a PowerShell script that:

- accepts a required tag argument
- strips a leading `v`
- reads the first `<version>` element from the root `pom.xml`
- exits non-zero when the values differ

Start with a minimal script and deliberately test it against a mismatched tag first.

**Step 2: Run the script to verify it fails**

Run: `powershell -ExecutionPolicy Bypass -File scripts/check-tag-version.ps1 v0.0.0`
Expected: FAIL with a clear version mismatch message.

**Step 3: Implement the final validation logic**

Refine the script so output is CI-friendly and the comparison is strict.

**Step 4: Run the script to verify it passes**

Run: `powershell -ExecutionPolicy Bypass -File scripts/check-tag-version.ps1 v2.5.0`
Expected: PASS with a message confirming the tag matches the project version.

**Step 5: Commit**

Run:

```bash
git add scripts/check-tag-version.ps1
git commit -m "build: add release tag version validation"
```

### Task 3: Update root Maven Central metadata

**Files:**
- Modify: `pom.xml`

**Step 1: Write the failing metadata check**

Manually inspect `pom.xml` and identify fields still pointing to upstream `xuxueli/xxl-job`.

Expected failing conditions:

- project URL points to upstream
- SCM fields point to upstream
- developer metadata points to upstream maintainer
- distribution management endpoint or server ID is not aligned with the planned workflow

**Step 2: Update the root POM**

Change:

- `<url>` to `https://github.com/falcon-jin/xxl-job`
- `<scm>` URLs and connections to `falcon-jin/xxl-job`
- `<developers>` to the current maintainer identity
- `distributionManagement` to the intended Maven Central publishing endpoint
- any required plugin properties for non-interactive CI signing if they are still incomplete

Do not broaden the release profile to publish non-library modules.

**Step 3: Run Maven help output to verify the POM still parses**

Run: `mvn -q -N help:evaluate -Dexpression=project.version -DforceStdout`
Expected: PASS and output `2.5.0` or the current target release version.

**Step 4: Run a deploy dry check without credentials**

Run: `mvn -B -P release -pl xxl-job-core -am -DskipTests package`
Expected: PASS for package lifecycle, confirming the profile still builds the publishable artifact.

**Step 5: Commit**

Run:

```bash
git add pom.xml
git commit -m "build: align Maven Central metadata"
```

### Task 4: Replace the build-only GitHub Actions workflow with a tag release workflow

**Files:**
- Modify: `.github/workflows/maven.yml`

**Step 1: Write the failing workflow behavior definition**

Document the desired failure cases directly in the workflow design while editing:

- non-tag pushes must not publish
- tag/version mismatch must fail
- missing secrets must fail during setup or deploy

**Step 2: Implement the workflow**

Update the workflow so it:

- triggers on `push.tags: ['v*']`
- uses `actions/checkout@v4`
- uses `actions/setup-java@v4`
- configures `server-id`, `server-username`, and `server-password`
- imports the GPG key from secrets
- runs the tag validation script
- runs `mvn -B -P release deploy`

Keep permissions minimal and cache Maven dependencies.

**Step 3: Validate workflow YAML locally**

Run: `Get-Content .github/workflows/maven.yml`
Expected: Human review confirms trigger, setup, validation, and deploy steps are present.

**Step 4: Validate release command locally**

Run: `mvn -B -P release -pl xxl-job-core -am package`
Expected: PASS locally without deploy, proving the workflow command is structurally sound.

**Step 5: Commit**

Run:

```bash
git add .github/workflows/maven.yml
git commit -m "ci: publish releases to Maven Central on tags"
```

### Task 5: Document release prerequisites for maintainers

**Files:**
- Modify: `README.md`

**Step 1: Write the failing documentation gap**

Identify that the repository currently does not tell maintainers:

- which secrets are required
- how tag naming maps to Maven version
- that only `xxl-job-core` is published

**Step 2: Add a release section**

Add concise maintainer documentation covering:

- required GitHub secrets
- required Maven Central / Sonatype setup
- required GPG setup
- release steps: bump version, commit, tag, push tag

**Step 3: Review the documentation**

Run: `Get-Content README.md`
Expected: Human review confirms the new release section is accurate and concise.

**Step 4: Commit**

Run:

```bash
git add README.md
git commit -m "docs: add Maven Central release instructions"
```

### Task 6: Run final verification before completion

**Files:**
- Verify: `pom.xml`
- Verify: `.github/workflows/maven.yml`
- Verify: `scripts/check-tag-version.ps1`
- Verify: `README.md`

**Step 1: Verify tag validation failure case**

Run: `powershell -ExecutionPolicy Bypass -File scripts/check-tag-version.ps1 v0.0.0`
Expected: FAIL with version mismatch.

**Step 2: Verify tag validation success case**

Run: `powershell -ExecutionPolicy Bypass -File scripts/check-tag-version.ps1 v2.5.0`
Expected: PASS if the current project version is `2.5.0`.

**Step 3: Verify Maven package build**

Run: `mvn -B -P release -pl xxl-job-core -am package`
Expected: PASS.

**Step 4: Verify effective workflow content**

Run: `Get-Content .github/workflows/maven.yml`
Expected: Confirm tag trigger, Java setup, GPG import, validation, and deploy command are present.

**Step 5: Verify git diff**

Run: `git diff -- scripts/check-tag-version.ps1 pom.xml .github/workflows/maven.yml README.md`
Expected: Only intended release-related changes appear.

**Step 6: Commit**

Run:

```bash
git add scripts/check-tag-version.ps1 pom.xml .github/workflows/maven.yml README.md
git commit -m "build: automate Maven Central releases"
```
