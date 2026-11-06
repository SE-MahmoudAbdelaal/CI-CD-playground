# deploy branch

Back4app Containers watches this branch and deploys every commit to it.

The CD workflow on `master` (`.github/workflows/cd.yml`) commits here after a new version
passes staging on the self-hosted runner. Each commit only changes the `FROM` line of the
`Dockerfile` to the image CI built and tested, for example
`ghcr.io/se-mahmoudabdelaal/ci-cd-demo:sha-3f1602e`.

To roll back, run the CD workflow by hand ("Run workflow") with an older image tag.
