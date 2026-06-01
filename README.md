# RallyHub 🏸

A mobile platform for managing social sports clubs — membership, sessions, credits, and tournaments.

## Architecture

```
rallyhub/
├── apps/
│   ├── mobile/      React Native (Expo) — iOS & Android
│   └── backend/     AWS Lambda (Node.js/TypeScript) + API Gateway
├── infra/           AWS CDK — Infrastructure as Code
└── packages/
    └── shared/      Shared TypeScript types & constants
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Mobile | React Native + Expo + TypeScript |
| State | Zustand + React Query |
| Backend | AWS Lambda + API Gateway (REST) |
| Auth | AWS Cognito (JWT) |
| Database | AWS DynamoDB |
| Notifications | Amazon SNS → APNs / FCM |
| Email | Amazon SES |
| Infrastructure | AWS CDK (TypeScript) |
| CI/CD | GitHub Actions |

## Getting Started

### Prerequisites
- Node.js 20+
- Yarn 1.22+
- AWS CLI configured
- Expo CLI (`npm install -g expo-cli`)

### Install
```bash
yarn install
```

### Mobile
```bash
yarn mobile
# or
cd apps/mobile && npx expo start
```

### Backend (local)
```bash
yarn backend:dev
```

### Infrastructure
```bash
cd infra
yarn synth      # Preview CloudFormation
yarn deploy     # Deploy to AWS
```

## Environments
- `development` — local / feature branches
- `staging` — merged to `main`
- `production` — tagged releases (`v*`)

## Contributing
1. Branch from `main`
2. Open a PR — CI runs lint → unit tests → integration tests
3. Staging deploy on merge to `main`
4. Production deploy on version tag
