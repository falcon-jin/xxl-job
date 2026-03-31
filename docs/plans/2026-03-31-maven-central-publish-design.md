# Maven Central Tag Publish Design

**Context**

This repository is a Maven multi-module project with a root aggregator `pom.xml` and these modules:

- `xxl-job-core`
- `xxl-job-admin`
- `xxl-job-executor-samples`

The repository already contains:

- a GitHub Actions workflow at `.github/workflows/maven.yml`
- a `release` Maven profile in the root POM
- source, javadoc, and GPG plugin configuration

The current CI only runs `mvn package`. It does not publish artifacts. The current publishing metadata still points to the upstream `xuxueli/xxl-job` repository and must be aligned with this fork before Maven Central release is enabled.

## Goal

Publish `xxl-job-core` to Maven Central automatically when a Git tag such as `v2.5.1` is pushed to GitHub.

## Scope

In scope:

- GitHub Actions release workflow for tag-triggered publishing
- Maven Central compatible metadata updates in POM files
- version/tag consistency validation
- secret-driven authentication and signing

Out of scope:

- automatic version bumping
- snapshot publishing on branch pushes
- publishing `xxl-job-admin`
- publishing `xxl-job-executor-samples`

## Release Strategy

Use Git tag pushes as the only production release trigger.

Example:

- project version in root POM: `2.5.1`
- pushed Git tag: `v2.5.1`

The workflow extracts the version from the tag, compares it with the root POM version, and fails immediately if they do not match.

This prevents accidental publication of the wrong artifact version.

## Artifact Strategy

Only `xxl-job-core` should be published to Maven Central.

Reasoning:

- `xxl-job-core` is a reusable library artifact intended for dependency consumption
- `xxl-job-admin` is an application artifact, not a general-purpose library
- `xxl-job-executor-samples` is example content and should not be published

The existing `maven.deploy.skip=true` configuration in non-library modules should remain in place. The `release` profile should continue to scope publishing to `xxl-job-core`.

## POM Changes

The root `pom.xml` must be updated to satisfy Maven Central metadata requirements and reflect the forked repository:

- set project URL to the fork repository URL
- update `scm.url`
- update `scm.connection`
- update `scm.developerConnection`
- update `developers` information to the current maintainer
- keep license metadata valid
- ensure `distributionManagement` points to the correct Central publishing endpoint and matches the Maven server ID used by CI

Child modules do not need broad structural changes. The key publication behavior should stay centralized in the root POM.

## CI Workflow Design

The GitHub Actions workflow should:

1. Trigger only on pushed tags matching `v*`
2. Check out the repository
3. Set up Temurin JDK 8
4. Configure Maven settings with the publishing server credentials
5. Import the GPG private key from GitHub Secrets
6. Validate that the tag version matches the root POM version
7. Run `mvn -B -P release deploy`

Recommended workflow properties:

- minimal permissions, `contents: read`
- Maven dependency cache enabled
- explicit environment variables for GPG passphrase and Maven credentials

## Secrets

The workflow depends on these GitHub Actions secrets:

- `MAVEN_USERNAME`
- `MAVEN_PASSWORD`
- `MAVEN_GPG_PRIVATE_KEY`
- `MAVEN_GPG_PASSPHRASE`

These values must not be stored in the repository.

## Operational Workflow

Release flow for maintainers:

1. Update the project version in the root and module POM hierarchy
2. Commit the version change
3. Create a tag such as `v2.5.1`
4. Push the tag to GitHub
5. GitHub Actions publishes `xxl-job-core` to Maven Central

## Preconditions

Before first release:

- the `io.github.falcon-jin` namespace must be verified in Maven Central / Sonatype
- a GPG keypair for signing must exist
- the required secrets must be configured in the GitHub repository

## Failure Handling

The workflow should fail fast for:

- tag version and POM version mismatch
- missing or invalid Maven credentials
- missing or invalid GPG signing material
- Central-side validation errors

Fail-fast behavior is preferred over partial or ambiguous publication states.

## Expected Outcome

After implementation:

- normal branch pushes continue to build without publishing
- pushing a release tag triggers an authenticated, signed Maven deploy
- `xxl-job-core` is the only published Central artifact
- published metadata points to `falcon-jin/xxl-job`, not the upstream project repository
