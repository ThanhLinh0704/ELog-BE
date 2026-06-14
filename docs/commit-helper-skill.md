---
name: commit-helper
description: Assists the user in analyzing workspace changes, generating high-quality conventional commit messages, and outputting exact Git commands for the user to execute. Strictly forbids the agent from running commit or push commands directly.
---

# Commit Helper Skill

This skill teaches the agent how to help the user commit code safely, professionally, and in accordance with conventional commit standards, while strictly honoring the constraint to never commit or push code automatically.

## Core Constraint
> [!IMPORTANT]
> **NEVER run `git commit` or `git push` commands directly.**
> The agent must always present the exact commands to the user in a copy-pastable code block so that the user can execute them manually.

---

## When to Use This Skill
Use this skill when the user:
- Says they want to commit code.
- Asks for a commit message recommendation.
- Finishes a task and asks what to do next.
- Wants to stage files or needs guidance on organizing their changes.

---

## Commit Format Guidelines
The agent must generate commit messages following the **Conventional Commits** specification:

```
<type>(<scope>): <description>

[optional body]

[optional footer(s)]
```

### 1. Types
- `feat`: A new feature (e.g., adding an API endpoint, new service).
- `fix`: A bug fix.
- `docs`: Documentation changes only (e.g., updating README, adding API docs).
- `style`: Changes that do not affect the meaning of the code (formatting, white-space, missing semi-colons).
- `refactor`: A code change that neither fixes a bug nor adds a feature.
- `perf`: A code change that improves performance.
- `test`: Adding missing tests or correcting existing tests.
- `build`: Changes that affect the build system or external dependencies (e.g., `pom.xml` changes).
- `ci`: Changes to CI configuration files and scripts.
- `chore`: Other changes that don't modify src or test files.
- `revert`: Reverts a previous commit.

### 2. Scope
The scope should represent the part of the codebase affected:
- `db`: Database migrations or configurations.
- `auth`: Security, JWT, user authentication.
- `route`: Predefined routing or logistics logic.
- `api`: API controllers or request/response envelopes.
- `config`: Environment configuration or `application.yml`.
- `us01`: Specific to User Story 01 skeleton/checklist.
- Or any relevant module name (e.g., `service`, `repository`).

### 3. Description (Subject)
- Use the imperative, present tense: "change" not "changed" nor "changes".
- Do not capitalize the first letter.
- Do not put a period (`.`) at the end.

---

## Workflow Steps for the Agent

### Step 1: Analyze Current Git State
Run the following commands to understand what files are modified and staged:
```bash
git status
```
If there are staged changes, inspect them:
```bash
git diff --cached
```
If there are unstaged changes, inspect them to understand what has been modified:
```bash
git diff
```

### Step 2: Formulate the Commit Message
Based on the file changes and the task done:
1. Identify the primary `type` and `scope`.
2. Write a concise description of the changes.
3. If there are multiple logical changes, group them in the commit body as bullet points.

### Step 3: Present Commands and Explanation
Present the findings to the user. Do not run the modifying commands. Provide them in a formatted block.

Example response layout:
```markdown
Dưới đây là các lệnh git để bạn tự thực hiện commit:

### 1. Stage các file đã thay đổi
```bash
git add <tên_file_hoặc_.>
```

### 2. Commit code với thông điệp chuẩn hóa
```bash
git commit -m "feat(api): add trip validation endpoint"
```

### 3. Push lên repository
```bash
git push origin <tên_nhánh>
```

**Giải thích thông điệp commit:**
- **type**: `feat` vì đây là tính năng mới.
- **scope**: `api` do chỉnh sửa controller.
- **nội dung**: Mô tả ngắn gọn việc thêm endpoint xác thực hành trình.
```
