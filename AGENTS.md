# Agent instructions

This repo is built by four people, each with their own agent. Before doing any work:

1. Check for a file named `.team-member` in the repo root.
2. If it is missing, ask the user: "Which member are you? Reply 1, 2, 3 or 4." Use the AskQuestion tool if you have it. Write only the digit to `.team-member`. The file is git-ignored, so do not commit it.
3. If it exists, read it and do not ask again.
4. Read [docs/TEAM_PLAN.md](docs/TEAM_PLAN.md): the **Common** section, then **only your member section** (`## Member N`). Do not implement other members' sections.
5. Find your work in your member section of TEAM_PLAN.md and the ownership table. There are no stubs or `TODO(Member N)` markers: create your own files using the names, signatures and types in Common and "Integration details".
6. Only edit files listed under your member in the ownership table. If you need a change elsewhere, stop and tell the user to raise it with the group.
7. Do not change the contracts in the Common section (signatures, names, HTTP mapping) without agreement from the whole group.
8. Before saying you are done, run `./mvnw clean verify`. The Testcontainers tests need Docker running.
