#!/usr/bin/env node
'use strict';

const fs = require('node:fs');
const path = require('node:path');

const inputPath = process.argv[2];
const outputPath = process.argv[3];
if (!inputPath || !outputPath) {
  process.stderr.write('Cách dùng: node ua-assemble-scan.js <scan-input> <scan-output>\n');
  process.exit(1);
}

try {
  const source = JSON.parse(fs.readFileSync(path.resolve(inputPath), 'utf8'));
  if (source.scriptCompleted !== true) throw new Error('Discovery script chưa hoàn tất thành công.');
  if (!Array.isArray(source.files) || source.totalFiles !== source.files.length) {
    throw new Error('totalFiles không khớp với files.length.');
  }
  if (!source.importMap || Object.keys(source.importMap).length !== source.files.length) {
    throw new Error('importMap không bao phủ đầy đủ danh sách files.');
  }

  const description = 'ELog Delivery Management System là backend quản lý giao nhận hàng điện tử cho doanh nghiệp SME tại Việt Nam, được thiết kế cho mô hình một kho, tuyến cố định và giao hàng đến cửa hàng bằng Spring Boot. Lưu ý: dự án có hơn 100 tệp nguồn; nên giới hạn phạm vi phân tích vào một thư mục con để xử lý nhanh hơn.';
  const finalResult = {
    name: source.name,
    description,
    languages: source.languages,
    frameworks: source.frameworks,
    files: source.files,
    totalFiles: source.totalFiles,
    filteredByIgnore: source.filteredByIgnore,
    estimatedComplexity: source.estimatedComplexity,
    importMap: source.importMap,
  };

  fs.mkdirSync(path.dirname(path.resolve(outputPath)), { recursive: true });
  fs.writeFileSync(path.resolve(outputPath), `${JSON.stringify(finalResult, null, 2)}\n`, 'utf8');
  process.exit(0);
} catch (error) {
  process.stderr.write(`${error && error.stack ? error.stack : error}\n`);
  process.exit(1);
}
