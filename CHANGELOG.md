# Changelog

All notable changes to this project are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project aims
to follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.1] - 2026-08-25

### Fixed

- OAuth sign-in no longer loses access after a while. The access token was never
  being refreshed, so a signed-in session stopped working once the token expired
  or the app was reopened from the background. Token refresh now runs on
  Bitbucket's 401 response.

## [0.1.0] - 2026-08-25

### Added

- Sign in with an Atlassian API token or with Bitbucket (OAuth, via a stateless
  Cloudflare Worker). Multiple accounts at once.
- Reviewer inbox aggregated across all accounts and workspaces, filtered by
  To review, Mine, or All.
- Pull request review: files-changed diff, inline comments, approve, request
  changes, merge, decline, and comment threads with resolve.
- Repository explorer: file and folder browsing, a syntax-highlighted viewer with
  wrap, find, and outline, plus branches, commits, diffs, a fuzzy file finder, and
  rendered Markdown.
- Pipelines: runs, steps, in-app logs, run, stop, re-run, deployments, and schedules.
- Code search across a workspace or within a repository.
- One Account screen for accounts and settings, light and dark themes, and a
  biometric app lock.
