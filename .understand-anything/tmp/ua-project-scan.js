#!/usr/bin/env node
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { spawnSync } = require('node:child_process');
const { pathToFileURL } = require('node:url');

const LANGUAGE_BY_EXTENSION = new Map([
  ['.ts', 'typescript'], ['.tsx', 'typescript'],
  ['.js', 'javascript'], ['.jsx', 'javascript'],
  ['.py', 'python'], ['.go', 'go'], ['.rs', 'rust'], ['.java', 'java'],
  ['.rb', 'ruby'], ['.cpp', 'cpp'], ['.cc', 'cpp'], ['.cxx', 'cpp'],
  ['.h', 'cpp'], ['.hpp', 'cpp'], ['.c', 'c'], ['.cs', 'csharp'],
  ['.swift', 'swift'], ['.kt', 'kotlin'], ['.php', 'php'], ['.vue', 'vue'],
  ['.svelte', 'svelte'], ['.sh', 'shell'], ['.bash', 'shell'],
  ['.ps1', 'powershell'], ['.bat', 'batch'], ['.cmd', 'batch'],
  ['.md', 'markdown'], ['.rst', 'markdown'], ['.yaml', 'yaml'], ['.yml', 'yaml'],
  ['.json', 'json'], ['.jsonc', 'jsonc'], ['.toml', 'toml'], ['.sql', 'sql'],
  ['.graphql', 'graphql'], ['.gql', 'graphql'], ['.proto', 'protobuf'],
  ['.tf', 'terraform'], ['.tfvars', 'terraform'], ['.html', 'html'], ['.htm', 'html'],
  ['.css', 'css'], ['.scss', 'css'], ['.sass', 'css'], ['.less', 'css'],
  ['.xml', 'xml'], ['.cfg', 'config'], ['.ini', 'config'], ['.env', 'config'],
]);

const EXCLUDED_DIRECTORY_SEGMENTS = new Set([
  'node_modules', '.git', 'vendor', 'venv', '.venv', '__pycache__',
  'dist', 'build', 'out', 'coverage', '.next', '.cache', '.turbo', 'target', 'obj',
  '.idea', '.vscode',
]);
const EXCLUDED_BINARY_EXTENSIONS = new Set([
  '.png', '.jpg', '.jpeg', '.gif', '.svg', '.ico', '.woff', '.woff2', '.ttf', '.eot',
  '.mp3', '.mp4', '.pdf', '.zip', '.tar', '.gz',
]);
const DOC_EXTENSIONS = new Set(['.md', '.rst', '.txt']);
const CONFIG_EXTENSIONS = new Set(['.yaml', '.yml', '.json', '.jsonc', '.toml', '.xml', '.cfg', '.ini', '.env']);
const DATA_EXTENSIONS = new Set(['.sql', '.graphql', '.gql', '.proto', '.prisma', '.csv']);
const SCRIPT_EXTENSIONS = new Set(['.sh', '.bash', '.ps1', '.bat']);
const MARKUP_EXTENSIONS = new Set(['.html', '.htm', '.css', '.scss', '.sass', '.less']);
const SOURCE_PROBES = ['.ts', '.tsx', '.js', '.jsx', '/index.ts', '/index.js', '/index.tsx', '/index.jsx', '.py', '.go', '.rs', '.rb'];

function toPosix(value) {
  return value.replace(/\\/g, '/').replace(/^\.\//, '');
}

function normalizeRelative(value) {
  const normalized = path.posix.normalize(toPosix(value));
  return normalized === '.' ? '' : normalized.replace(/^\/+/, '');
}

function safeReadText(filePath) {
  try {
    return fs.readFileSync(filePath, 'utf8').replace(/^\uFEFF/, '');
  } catch (error) {
    process.stderr.write(`Cảnh báo: không đọc được ${filePath}: ${error.message}\n`);
    return '';
  }
}

function isRealFile(filePath) {
  try {
    return fs.statSync(filePath).isFile();
  } catch {
    return false;
  }
}

function discoverWithGit(projectRoot) {
  const result = spawnSync('git', ['ls-files', '-z'], {
    cwd: projectRoot,
    encoding: 'buffer',
    maxBuffer: 256 * 1024 * 1024,
    windowsHide: true,
  });
  if (result.error || result.status !== 0) return null;
  return result.stdout.toString('utf8').split('\0').filter(Boolean).map(toPosix);
}

function discoverRecursively(projectRoot) {
  const files = [];
  const stack = [''];
  while (stack.length > 0) {
    const relativeDirectory = stack.pop();
    const absoluteDirectory = path.join(projectRoot, relativeDirectory);
    let entries;
    try {
      entries = fs.readdirSync(absoluteDirectory, { withFileTypes: true });
    } catch (error) {
      process.stderr.write(`Cảnh báo: không thể liệt kê ${absoluteDirectory}: ${error.message}\n`);
      continue;
    }
    entries.sort((a, b) => a.name.localeCompare(b.name));
    for (let index = entries.length - 1; index >= 0; index -= 1) {
      const entry = entries[index];
      const relativePath = normalizeRelative(path.posix.join(toPosix(relativeDirectory), entry.name));
      if (entry.isDirectory()) {
        stack.push(relativePath);
      } else if (entry.isFile() || entry.isSymbolicLink()) {
        files.push(relativePath);
      }
    }
  }
  return files;
}

function isBaselineIgnored(relativePath) {
  const normalized = normalizeRelative(relativePath);
  const segments = normalized.split('/');
  const baseName = segments[segments.length - 1];
  const lowerBase = baseName.toLowerCase();
  const extension = path.posix.extname(lowerBase);

  if (segments.slice(0, -1).some((segment) => EXCLUDED_DIRECTORY_SEGMENTS.has(segment))) return true;
  if (lowerBase.endsWith('.lock') || ['package-lock.json', 'yarn.lock', 'pnpm-lock.yaml'].includes(lowerBase)) return true;
  if (EXCLUDED_BINARY_EXTENSIONS.has(extension)) return true;
  if (lowerBase.endsWith('.min.js') || lowerBase.endsWith('.min.css') || lowerBase.endsWith('.map') || lowerBase.includes('.generated.')) return true;
  if (baseName === 'LICENSE' || baseName === '.gitignore' || baseName === '.editorconfig' || baseName === '.prettierrc') return true;
  if (baseName.startsWith('.eslintrc') || lowerBase.endsWith('.log')) return true;
  return false;
}

async function loadUnifiedIgnoreFilter(projectRoot) {
  const hasProjectIgnore = fs.existsSync(path.join(projectRoot, '.understand-anything', '.understandignore'));
  const hasRootIgnore = fs.existsSync(path.join(projectRoot, '.understandignore'));
  if (!hasProjectIgnore && !hasRootIgnore) return null;

  const userProfile = process.env.USERPROFILE || process.env.HOMEDRIVE && process.env.HOMEPATH
    ? `${process.env.HOMEDRIVE || ''}${process.env.HOMEPATH || ''}`
    : '';
  const candidates = [
    process.env.UNDERSTAND_ANYTHING_CORE_DIST,
    userProfile && path.join(userProfile, '.understand-anything', 'repo', 'understand-anything-plugin', 'packages', 'core', 'dist', 'index.js'),
    userProfile && path.join(userProfile, '.codex', 'understand-anything', 'understand-anything-plugin', 'packages', 'core', 'dist', 'index.js'),
  ].filter(Boolean);

  const corePath = candidates.find((candidate) => fs.existsSync(candidate));
  if (!corePath) {
    throw new Error('Không tìm thấy @understand-anything/core để áp dụng .understandignore.');
  }
  const core = await import(pathToFileURL(corePath).href);
  if (typeof core.createIgnoreFilter !== 'function') {
    throw new Error('@understand-anything/core không export createIgnoreFilter.');
  }
  return core.createIgnoreFilter(projectRoot);
}

function languageFor(relativePath) {
  const baseName = path.posix.basename(relativePath);
  if (baseName === 'Dockerfile') return 'dockerfile';
  if (baseName === 'Makefile') return 'makefile';
  if (baseName === 'Jenkinsfile') return 'jenkinsfile';
  if (baseName === '.env') return 'config';
  const extension = path.posix.extname(baseName).toLowerCase();
  if (LANGUAGE_BY_EXTENSION.has(extension)) return LANGUAGE_BY_EXTENSION.get(extension);
  return extension ? extension.slice(1) : 'unknown';
}

function isInfrastructurePath(relativePath) {
  const normalized = normalizeRelative(relativePath);
  const lower = normalized.toLowerCase();
  const baseName = path.posix.basename(normalized);
  const lowerBase = baseName.toLowerCase();
  const extension = path.posix.extname(lowerBase);
  const segments = lower.split('/');
  return baseName === 'Dockerfile'
    || baseName === 'Makefile'
    || baseName === 'Jenkinsfile'
    || baseName === 'Procfile'
    || baseName === 'Vagrantfile'
    || lowerBase.startsWith('docker-compose.')
    || extension === '.tf'
    || extension === '.tfvars'
    || lower === '.gitlab-ci.yml'
    || lower.startsWith('.circleci/')
    || lower.startsWith('.github/workflows/')
    || lowerBase.endsWith('.k8s.yaml')
    || lowerBase.endsWith('.k8s.yml')
    || segments.includes('k8s')
    || segments.includes('kubernetes');
}

function categoryFor(relativePath) {
  const normalized = normalizeRelative(relativePath);
  const baseName = path.posix.basename(normalized);
  const lowerBase = baseName.toLowerCase();
  const extension = path.posix.extname(lowerBase);

  if (isInfrastructurePath(normalized)) return 'infra';
  if (DATA_EXTENSIONS.has(extension) || lowerBase.endsWith('.schema.json')) return 'data';
  if (DOC_EXTENSIONS.has(extension) && baseName !== 'LICENSE') return 'docs';
  if (CONFIG_EXTENSIONS.has(extension)
      || lowerBase === 'tsconfig.json'
      || lowerBase === 'package.json'
      || lowerBase === 'pyproject.toml'
      || lowerBase === 'cargo.toml'
      || lowerBase === 'go.mod'
      || lowerBase === '.env'
      || lowerBase.startsWith('.env.')) return 'config';
  if (SCRIPT_EXTENSIONS.has(extension)) return 'script';
  if (MARKUP_EXTENSIONS.has(extension)) return 'markup';
  return 'code';
}

function findWcExecutable() {
  const candidates = [];
  if (process.env.WC_EXE) candidates.push(process.env.WC_EXE);

  const whereWc = spawnSync('where.exe', ['wc.exe'], { encoding: 'utf8', windowsHide: true });
  if (whereWc.status === 0) candidates.push(...whereWc.stdout.split(/\r?\n/).filter(Boolean));

  const whereGit = spawnSync('where.exe', ['git.exe'], { encoding: 'utf8', windowsHide: true });
  if (whereGit.status === 0) {
    for (const gitPath of whereGit.stdout.split(/\r?\n/).filter(Boolean)) {
      const gitRoot = path.dirname(path.dirname(gitPath));
      candidates.push(path.join(gitRoot, 'usr', 'bin', 'wc.exe'));
    }
  }
  if (process.env.ProgramFiles) candidates.push(path.join(process.env.ProgramFiles, 'Git', 'usr', 'bin', 'wc.exe'));
  candidates.push('wc');

  for (const candidate of candidates) {
    if (candidate !== 'wc' && !fs.existsSync(candidate)) continue;
    const probe = spawnSync(candidate, ['--version'], { encoding: 'utf8', windowsHide: true });
    if (!probe.error && probe.status === 0) return candidate;
  }
  return null;
}

function directNewlineCount(absolutePath) {
  try {
    const buffer = fs.readFileSync(absolutePath);
    let count = 0;
    for (const byte of buffer) if (byte === 10) count += 1;
    return count;
  } catch (error) {
    process.stderr.write(`Cảnh báo: không đếm được dòng của ${absolutePath}: ${error.message}\n`);
    return 0;
  }
}

function countLines(projectRoot, relativePaths) {
  const counts = new Map();
  const wcExecutable = findWcExecutable();
  if (!wcExecutable) {
    process.stderr.write('Cảnh báo: không tìm thấy wc; dùng phép đếm newline tương đương.\n');
    for (const relativePath of relativePaths) counts.set(relativePath, directNewlineCount(path.join(projectRoot, relativePath)));
    return counts;
  }

  const batchSize = relativePaths.length >= 500 ? 60 : 75;
  for (let offset = 0; offset < relativePaths.length; offset += batchSize) {
    const batch = relativePaths.slice(offset, offset + batchSize);
    const result = spawnSync(wcExecutable, ['-l', '--', ...batch], {
      cwd: projectRoot,
      encoding: 'utf8',
      maxBuffer: 64 * 1024 * 1024,
      windowsHide: true,
    });
    const outputLines = result.status === 0 ? result.stdout.trimEnd().split(/\r?\n/) : [];
    let parsedBatch = result.status === 0 && outputLines.length >= batch.length;
    if (parsedBatch) {
      for (let index = 0; index < batch.length; index += 1) {
        const match = outputLines[index].match(/^\s*(\d+)\s/);
        if (!match) {
          parsedBatch = false;
          break;
        }
        counts.set(batch[index], Number(match[1]));
      }
    }
    if (!parsedBatch) {
      for (const relativePath of batch) {
        const single = spawnSync(wcExecutable, ['-l', '--', relativePath], {
          cwd: projectRoot,
          encoding: 'utf8',
          maxBuffer: 16 * 1024 * 1024,
          windowsHide: true,
        });
        const match = single.status === 0 ? single.stdout.match(/^\s*(\d+)\s/) : null;
        counts.set(relativePath, match ? Number(match[1]) : directNewlineCount(path.join(projectRoot, relativePath)));
      }
    }
  }
  return counts;
}

function parseJsonFile(filePath) {
  try {
    return JSON.parse(fs.readFileSync(filePath, 'utf8').replace(/^\uFEFF/, ''));
  } catch {
    return null;
  }
}

function firstTomlValue(content, sectionName, key) {
  const escapedSection = sectionName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const section = content.match(new RegExp(`^\\[${escapedSection}\\]\\s*$([\\s\\S]*?)(?=^\\[|\\Z)`, 'mi'));
  if (!section) return '';
  const value = section[1].match(new RegExp(`^\\s*${key}\\s*=\\s*["']([^"']+)["']`, 'mi'));
  return value ? value[1].trim() : '';
}

function addKeywordFrameworks(content, definitions, frameworks) {
  const lower = content.toLowerCase();
  for (const [keyword, displayName] of definitions) {
    if (lower.includes(keyword.toLowerCase())) frameworks.add(displayName);
  }
}

function detectMetadata(projectRoot, relativePaths) {
  const fileSet = new Set(relativePaths);
  const frameworks = new Set();
  let packageJson = null;
  let cargoContent = '';
  let goModContent = '';
  let pyprojectContent = '';
  let pomContent = '';

  if (fs.existsSync(path.join(projectRoot, 'package.json'))) {
    packageJson = parseJsonFile(path.join(projectRoot, 'package.json'));
    if (packageJson) {
      const dependencyNames = Object.keys({ ...(packageJson.dependencies || {}), ...(packageJson.devDependencies || {}) });
      const npmFrameworks = new Map([
        ['react', 'React'], ['vue', 'Vue'], ['svelte', 'Svelte'], ['@angular/core', 'Angular'],
        ['express', 'Express'], ['fastify', 'Fastify'], ['koa', 'Koa'], ['next', 'Next.js'],
        ['nuxt', 'Nuxt'], ['vite', 'Vite'], ['vitest', 'Vitest'], ['jest', 'Jest'],
        ['mocha', 'Mocha'], ['tailwindcss', 'Tailwind CSS'], ['prisma', 'Prisma'],
        ['typeorm', 'TypeORM'], ['sequelize', 'Sequelize'], ['mongoose', 'Mongoose'],
        ['redux', 'Redux'], ['zustand', 'Zustand'], ['mobx', 'MobX'],
      ]);
      for (const dependencyName of dependencyNames) {
        if (npmFrameworks.has(dependencyName)) frameworks.add(npmFrameworks.get(dependencyName));
      }
    }
  }

  if (fs.existsSync(path.join(projectRoot, 'Cargo.toml'))) {
    cargoContent = safeReadText(path.join(projectRoot, 'Cargo.toml'));
    addKeywordFrameworks(cargoContent, [
      ['actix-web', 'Actix Web'], ['axum', 'Axum'], ['rocket', 'Rocket'], ['diesel', 'Diesel'],
      ['tokio', 'Tokio'], ['serde', 'Serde'], ['warp', 'Warp'],
    ], frameworks);
  }

  if (fs.existsSync(path.join(projectRoot, 'go.mod'))) {
    goModContent = safeReadText(path.join(projectRoot, 'go.mod'));
    addKeywordFrameworks(goModContent, [
      ['github.com/gin-gonic/gin', 'Gin'], ['github.com/labstack/echo', 'Echo'],
      ['github.com/gofiber/fiber', 'Fiber'], ['github.com/go-chi/chi', 'Chi'], ['gorm.io/gorm', 'GORM'],
    ], frameworks);
  }

  const pythonSources = [];
  for (const candidate of ['requirements.txt', 'pyproject.toml', 'setup.py', 'setup.cfg', 'Pipfile']) {
    const absolutePath = path.join(projectRoot, candidate);
    if (fs.existsSync(absolutePath)) {
      const content = safeReadText(absolutePath);
      pythonSources.push(content);
      if (candidate === 'pyproject.toml') pyprojectContent = content;
    }
  }
  addKeywordFrameworks(pythonSources.join('\n'), [
    ['django', 'Django'], ['djangorestframework', 'Django REST Framework'], ['fastapi', 'FastAPI'],
    ['flask', 'Flask'], ['sqlalchemy', 'SQLAlchemy'], ['alembic', 'Alembic'], ['celery', 'Celery'],
    ['pydantic', 'Pydantic'], ['uvicorn', 'Uvicorn'], ['gunicorn', 'Gunicorn'], ['aiohttp', 'aiohttp'],
    ['tornado', 'Tornado'], ['starlette', 'Starlette'], ['pytest', 'pytest'],
    ['hypothesis', 'Hypothesis'], ['channels', 'Django Channels'],
  ], frameworks);

  const gemfilePath = path.join(projectRoot, 'Gemfile');
  if (fs.existsSync(gemfilePath)) {
    addKeywordFrameworks(safeReadText(gemfilePath), [
      ['rails', 'Rails'], ['railties', 'Railties'], ['sinatra', 'Sinatra'], ['grape', 'Grape'],
      ['rspec', 'RSpec'], ['sidekiq', 'Sidekiq'], ['activerecord', 'Active Record'],
      ['actionpack', 'Action Pack'], ['devise', 'Devise'], ['pundit', 'Pundit'],
    ], frameworks);
  }

  for (const candidate of ['pom.xml', 'build.gradle', 'build.gradle.kts']) {
    const absolutePath = path.join(projectRoot, candidate);
    if (fs.existsSync(absolutePath)) {
      const content = safeReadText(absolutePath);
      if (candidate === 'pom.xml') pomContent = content;
      addKeywordFrameworks(content, [
        ['spring-boot', 'Spring Boot'], ['spring-web', 'Spring Web'], ['spring-data', 'Spring Data'],
        ['quarkus', 'Quarkus'], ['micronaut', 'Micronaut'], ['hibernate', 'Hibernate'],
        ['jakarta', 'Jakarta'], ['junit', 'JUnit'], ['ktor', 'Ktor'],
      ], frameworks);
    }
  }

  if (relativePaths.some((entry) => path.posix.basename(entry) === 'Dockerfile')) frameworks.add('Docker');
  if (relativePaths.some((entry) => /^docker-compose\.(?:yml|yaml)$/i.test(path.posix.basename(entry)))) frameworks.add('Docker Compose');
  if (relativePaths.some((entry) => path.posix.extname(entry).toLowerCase() === '.tf')) frameworks.add('Terraform');
  if (relativePaths.some((entry) => /^\.github\/workflows\/.*\.ya?ml$/i.test(entry))) frameworks.add('GitHub Actions');
  if (fileSet.has('.gitlab-ci.yml')) frameworks.add('GitLab CI');
  if (relativePaths.some((entry) => path.posix.basename(entry) === 'Jenkinsfile')) frameworks.add('Jenkins');

  let name = packageJson && typeof packageJson.name === 'string' ? packageJson.name.trim() : '';
  if (!name && cargoContent) name = firstTomlValue(cargoContent, 'package', 'name');
  if (!name && goModContent) {
    const moduleMatch = goModContent.match(/^\s*module\s+(\S+)/m);
    if (moduleMatch) name = moduleMatch[1].split('/').filter(Boolean).pop() || '';
  }
  if (!name && pyprojectContent) {
    name = firstTomlValue(pyprojectContent, 'project', 'name') || firstTomlValue(pyprojectContent, 'tool.poetry', 'name');
  }
  if (!name && pomContent) {
    const nameMatch = pomContent.match(/<name>\s*([^<]+?)\s*<\/name>/i);
    if (nameMatch) name = nameMatch[1].trim();
    if (!name) {
      const withoutParent = pomContent.replace(/<parent\b[\s\S]*?<\/parent>/i, '');
      const artifactMatch = withoutParent.match(/<artifactId>\s*([^<]+?)\s*<\/artifactId>/i);
      if (artifactMatch) name = artifactMatch[1].trim();
    }
  }
  if (!name) name = path.basename(projectRoot);

  return {
    name,
    rawDescription: packageJson && typeof packageJson.description === 'string' ? packageJson.description.trim() : '',
    frameworks: [...frameworks].sort((a, b) => a.localeCompare(b)),
    goModule: (goModContent.match(/^\s*module\s+(\S+)/m) || [null, ''])[1],
  };
}

function probePath(candidate, fileSet, probes = SOURCE_PROBES) {
  const normalized = normalizeRelative(candidate);
  const results = [];
  if (fileSet.has(normalized)) results.push(normalized);
  if (!path.posix.extname(normalized)) {
    for (const suffix of probes) {
      const probed = normalizeRelative(`${normalized}${suffix}`);
      if (fileSet.has(probed)) results.push(probed);
    }
  }
  return results;
}

function parseTsConfig(projectRoot) {
  const configPath = path.join(projectRoot, 'tsconfig.json');
  if (!fs.existsSync(configPath)) return { baseUrl: '', paths: {} };
  let content = safeReadText(configPath);
  content = content.replace(/\/\*[\s\S]*?\*\//g, '').replace(/^\s*\/\/.*$/gm, '').replace(/,\s*([}\]])/g, '$1');
  try {
    const config = JSON.parse(content);
    return {
      baseUrl: toPosix(config.compilerOptions && config.compilerOptions.baseUrl || ''),
      paths: config.compilerOptions && config.compilerOptions.paths || {},
    };
  } catch {
    return { baseUrl: '', paths: {} };
  }
}

function resolveTsAlias(specifier, tsConfig, fileSet) {
  const resolved = [];
  for (const [aliasPattern, targets] of Object.entries(tsConfig.paths || {})) {
    const starIndex = aliasPattern.indexOf('*');
    let captured = '';
    if (starIndex >= 0) {
      const prefix = aliasPattern.slice(0, starIndex);
      const suffix = aliasPattern.slice(starIndex + 1);
      if (!specifier.startsWith(prefix) || !specifier.endsWith(suffix)) continue;
      captured = specifier.slice(prefix.length, specifier.length - suffix.length || undefined);
    } else if (specifier !== aliasPattern) {
      continue;
    }
    for (const targetPattern of Array.isArray(targets) ? targets : []) {
      const target = starIndex >= 0 ? targetPattern.replace('*', captured) : targetPattern;
      const candidate = path.posix.join(tsConfig.baseUrl || '', toPosix(target));
      resolved.push(...probePath(candidate, fileSet));
    }
  }
  return resolved;
}

function resolveJavaLikeImport(fqcn, extension, fileSet, allowTrim) {
  const matches = [];
  const segments = fqcn.split('.').filter(Boolean);
  const minimum = allowTrim ? 1 : segments.length;
  for (let length = segments.length; length >= minimum; length -= 1) {
    const suffix = `${segments.slice(0, length).join('/')}${extension}`;
    const found = [...fileSet].filter((candidate) => candidate === suffix || candidate.endsWith(`/${suffix}`));
    if (found.length > 0) {
      matches.push(...found);
      break;
    }
  }
  return matches;
}

function resolvePythonModule(moduleName, importedNames, fileSet) {
  const base = moduleName.split('.').filter(Boolean).join('/');
  if (!base) return [];
  const moduleFile = `${base}.py`;
  const packageFile = `${base}/__init__.py`;
  if (fileSet.has(moduleFile)) return [moduleFile];
  if (!fileSet.has(packageFile)) return [];
  const matches = [packageFile];
  for (const importedName of importedNames) {
    const cleanName = importedName.replace(/\s+as\s+\w+$/i, '').trim();
    if (!/^\w+$/.test(cleanName) || cleanName === '*') continue;
    for (const candidate of [`${base}/${cleanName}.py`, `${base}/${cleanName}/__init__.py`]) {
      if (fileSet.has(candidate)) matches.push(candidate);
    }
  }
  return matches;
}

function resolvePythonRelative(fromFile, dots, moduleName, importedNames, fileSet) {
  let baseDirectory = path.posix.dirname(fromFile);
  for (let index = 1; index < dots.length; index += 1) baseDirectory = path.posix.dirname(baseDirectory);
  const moduleBase = moduleName ? path.posix.join(baseDirectory, moduleName.replace(/\./g, '/')) : baseDirectory;
  const matches = [];
  for (const candidate of [`${moduleBase}.py`, `${moduleBase}/__init__.py`]) {
    const normalized = normalizeRelative(candidate);
    if (fileSet.has(normalized)) matches.push(normalized);
  }
  const packageCandidate = normalizeRelative(`${moduleBase}/__init__.py`);
  if (fileSet.has(packageCandidate) || !moduleName) {
    for (const importedName of importedNames) {
      const cleanName = importedName.replace(/\s+as\s+\w+$/i, '').trim();
      if (!/^\w+$/.test(cleanName) || cleanName === '*') continue;
      for (const candidate of [`${moduleBase}/${cleanName}.py`, `${moduleBase}/${cleanName}/__init__.py`]) {
        const normalized = normalizeRelative(candidate);
        if (fileSet.has(normalized)) matches.push(normalized);
      }
    }
  }
  return matches;
}

function buildImportMap(projectRoot, fileRecords, metadata) {
  const fileSet = new Set(fileRecords.map((record) => record.path));
  const importMap = Object.fromEntries(fileRecords.map((record) => [record.path, []]));
  const tsConfig = parseTsConfig(projectRoot);
  let composerAutoload = {};
  const composerPath = path.join(projectRoot, 'composer.json');
  if (fs.existsSync(composerPath)) {
    const composer = parseJsonFile(composerPath);
    composerAutoload = composer && composer.autoload && composer.autoload['psr-4'] || {};
  }

  for (const record of fileRecords) {
    if (record.fileCategory !== 'code') continue;
    const absolutePath = path.join(projectRoot, record.path);
    const content = safeReadText(absolutePath);
    const resolved = new Set();

    if (record.language === 'typescript' || record.language === 'javascript') {
      const importPattern = /(?:import\s+(?:[\s\S]*?\s+from\s+)?|require\s*\()\s*['"]([^'"]+)['"]/g;
      for (const match of content.matchAll(importPattern)) {
        const specifier = match[1];
        if (specifier.startsWith('./') || specifier.startsWith('../')) {
          for (const target of probePath(path.posix.join(path.posix.dirname(record.path), specifier), fileSet)) resolved.add(target);
        } else {
          for (const target of resolveTsAlias(specifier, tsConfig, fileSet)) resolved.add(target);
        }
      }
    } else if (record.language === 'python') {
      const fromPattern = /^\s*from\s+([.]*)([\w.]*)\s+import\s+([^#\n]+)/gm;
      for (const match of content.matchAll(fromPattern)) {
        const importedNames = match[3].replace(/[()]/g, '').split(',').map((value) => value.trim()).filter(Boolean);
        const targets = match[1]
          ? resolvePythonRelative(record.path, match[1], match[2], importedNames, fileSet)
          : resolvePythonModule(match[2], importedNames, fileSet);
        for (const target of targets) resolved.add(target);
      }
      const importPattern = /^\s*import\s+([^#\n]+)/gm;
      for (const match of content.matchAll(importPattern)) {
        for (const item of match[1].split(',')) {
          const moduleName = item.replace(/\s+as\s+\w+$/i, '').trim();
          for (const target of resolvePythonModule(moduleName, [], fileSet)) resolved.add(target);
        }
      }
    } else if (record.language === 'go' && metadata.goModule) {
      const importPattern = /["`]([^"`]+)["`]/g;
      for (const match of content.matchAll(importPattern)) {
        if (match[1] !== metadata.goModule && !match[1].startsWith(`${metadata.goModule}/`)) continue;
        const packageDirectory = match[1] === metadata.goModule ? '' : match[1].slice(metadata.goModule.length + 1);
        for (const candidate of fileSet) {
          if (path.posix.dirname(candidate) === normalizeRelative(packageDirectory) && candidate.endsWith('.go')) resolved.add(candidate);
        }
      }
    } else if (record.language === 'rust') {
      for (const match of content.matchAll(/^\s*mod\s+(\w+)\s*;/gm)) {
        for (const candidate of [`${path.posix.dirname(record.path)}/${match[1]}.rs`, `${path.posix.dirname(record.path)}/${match[1]}/mod.rs`]) {
          const normalized = normalizeRelative(candidate);
          if (fileSet.has(normalized)) resolved.add(normalized);
        }
      }
      for (const match of content.matchAll(/^\s*use\s+(crate|super)::([\w:]+)/gm)) {
        const segments = match[2].split('::');
        const base = match[1] === 'crate' ? 'src' : path.posix.dirname(path.posix.dirname(record.path));
        for (let length = segments.length; length >= 1; length -= 1) {
          const stem = path.posix.join(base, ...segments.slice(0, length));
          const candidates = [`${stem}.rs`, `${stem}/mod.rs`].map(normalizeRelative).filter((candidate) => fileSet.has(candidate));
          if (candidates.length) {
            for (const candidate of candidates) resolved.add(candidate);
            break;
          }
        }
      }
    } else if (record.language === 'java') {
      const importPattern = /^\s*import\s+(static\s+)?([\w.]+)(\.\*)?\s*;/gm;
      for (const match of content.matchAll(importPattern)) {
        if (match[3] && !match[1]) {
          const packageSuffix = `${match[2].replace(/\./g, '/')}/`;
          for (const candidate of fileSet) {
            const index = candidate.lastIndexOf(packageSuffix);
            if (index >= 0 && candidate.endsWith('.java') && !candidate.slice(index + packageSuffix.length).includes('/')) resolved.add(candidate);
          }
        } else {
          for (const target of resolveJavaLikeImport(match[2], '.java', fileSet, Boolean(match[1]))) resolved.add(target);
        }
      }
    } else if (record.language === 'kotlin') {
      for (const match of content.matchAll(/^\s*import\s+([\w.]+)(?:\s+as\s+\w+)?\s*$/gm)) {
        for (const target of resolveJavaLikeImport(match[1], '.kt', fileSet, true)) resolved.add(target);
      }
    } else if (record.language === 'ruby') {
      for (const match of content.matchAll(/require_relative\s*\(?\s*['"]([^'"]+)['"]/g)) {
        for (const target of probePath(path.posix.join(path.posix.dirname(record.path), match[1]), fileSet, ['.rb'])) resolved.add(target);
      }
      for (const match of content.matchAll(/(?:^|\n)\s*require\s*\(?\s*['"]([^'"]+)['"]/g)) {
        for (const base of [`lib/${match[1]}`, `app/${match[1]}`, match[1]]) {
          for (const target of probePath(base, fileSet, ['.rb'])) resolved.add(target);
        }
      }
    } else if (record.language === 'php') {
      for (const match of content.matchAll(/^\s*use\s+([^;]+);/gm)) {
        const namespaceName = match[1].replace(/\s+as\s+\w+$/i, '').trim();
        for (const [prefix, directory] of Object.entries(composerAutoload)) {
          if (!namespaceName.startsWith(prefix)) continue;
          const suffix = namespaceName.slice(prefix.length).replace(/\\/g, '/');
          const directories = Array.isArray(directory) ? directory : [directory];
          for (const base of directories) {
            const candidate = normalizeRelative(path.posix.join(toPosix(base), `${suffix}.php`));
            if (fileSet.has(candidate)) resolved.add(candidate);
          }
        }
      }
    } else if (record.language === 'c' || record.language === 'cpp') {
      for (const match of content.matchAll(/^\s*#include\s*(["<])([^">]+)[">]/gm)) {
        const includePath = toPosix(match[2]);
        const candidates = [];
        if (match[1] === '"') candidates.push(path.posix.join(path.posix.dirname(record.path), includePath));
        candidates.push(`include/${includePath}`, `src/${includePath}`, includePath);
        for (const candidate of candidates) {
          for (const target of probePath(candidate, fileSet, ['.h', '.hpp', '.hxx', '.cuh'])) resolved.add(target);
        }
      }
    }

    resolved.delete(record.path);
    importMap[record.path] = [...resolved].sort((a, b) => a.localeCompare(b));
  }
  return importMap;
}

async function main() {
  const projectRootArgument = process.argv[2];
  const outputPathArgument = process.argv[3];
  if (!projectRootArgument || !outputPathArgument) {
    throw new Error('Cách dùng: node ua-project-scan.js <project-root> <output-json>');
  }

  const projectRoot = path.resolve(projectRootArgument);
  const outputPath = path.resolve(outputPathArgument);
  let rootStat;
  try {
    rootStat = fs.statSync(projectRoot);
  } catch (error) {
    throw new Error(`Không thể truy cập project root: ${error.message}`);
  }
  if (!rootStat.isDirectory()) throw new Error(`Project root không phải thư mục: ${projectRoot}`);

  const gitFiles = discoverWithGit(projectRoot);
  const originalFiles = [...new Set((gitFiles === null ? discoverRecursively(projectRoot) : gitFiles)
    .map(normalizeRelative)
    .filter(Boolean)
    .filter((relativePath) => isRealFile(path.join(projectRoot, relativePath))))]
    .sort((a, b) => a.localeCompare(b));

  const baselineFiles = originalFiles.filter((relativePath) => !isBaselineIgnored(relativePath));
  const baselineSet = new Set(baselineFiles);
  const unifiedIgnore = await loadUnifiedIgnoreFilter(projectRoot);
  const includedPaths = unifiedIgnore
    ? originalFiles.filter((relativePath) => !unifiedIgnore.isIgnored(relativePath))
    : baselineFiles;
  const filteredByIgnore = unifiedIgnore
    ? originalFiles.filter((relativePath) => baselineSet.has(relativePath) && unifiedIgnore.isIgnored(relativePath)).length
    : 0;

  const lineCounts = countLines(projectRoot, includedPaths);
  const files = includedPaths.map((relativePath) => ({
    path: relativePath,
    language: languageFor(relativePath),
    sizeLines: lineCounts.get(relativePath) || 0,
    fileCategory: categoryFor(relativePath),
  })).sort((a, b) => a.path.localeCompare(b.path));

  const metadata = detectMetadata(projectRoot, includedPaths);
  const importMap = buildImportMap(projectRoot, files, metadata);
  const languages = [...new Set(files.map((file) => file.language))].sort((a, b) => a.localeCompare(b));
  const totalFiles = files.length;
  const estimatedComplexity = totalFiles <= 30 ? 'small'
    : totalFiles <= 150 ? 'moderate'
      : totalFiles <= 500 ? 'large'
        : 'very-large';
  const readmePath = path.join(projectRoot, 'README.md');
  const readmeHead = fs.existsSync(readmePath)
    ? safeReadText(readmePath).split(/\r?\n/).slice(0, 10).join('\n')
    : '';

  const output = {
    scriptCompleted: true,
    name: metadata.name,
    rawDescription: metadata.rawDescription,
    readmeHead,
    languages,
    frameworks: metadata.frameworks,
    files,
    totalFiles,
    filteredByIgnore,
    estimatedComplexity,
    importMap,
  };

  fs.mkdirSync(path.dirname(outputPath), { recursive: true });
  fs.writeFileSync(outputPath, `${JSON.stringify(output, null, 2)}\n`, 'utf8');
}

main().then(() => {
  process.exit(0);
}).catch((error) => {
  process.stderr.write(`${error && error.stack ? error.stack : error}\n`);
  process.exit(1);
});
