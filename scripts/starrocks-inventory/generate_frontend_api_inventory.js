#!/usr/bin/env node

const fs = require("fs");
const path = require("path");
const childProcess = require("child_process");
const ts = require("typescript");

const EXPECTED_FRONTEND_SHA = "5eab200650ecc2f111b94fea31f56a2f50a6ad22";
const HTTP_METHODS = new Set(["get", "post", "put", "patch", "delete"]);
const NON_API_METHODS = new Set(["addErrorHandler", "removeErrorHandler"]);

function parseArguments(argv) {
    const arguments = {
        check: false,
        frontendRoot: null,
        output: path.resolve(
            __dirname,
            "../../docs/starrocks-frontend-api-inventory.tsv",
        ),
    };
    for (let index = 0; index < argv.length; index += 1) {
        const argument = argv[index];
        if (argument === "--check") {
            arguments.check = true;
        } else if (argument === "--frontend-root") {
            arguments.frontendRoot = path.resolve(argv[++index]);
        } else if (argument === "--output") {
            arguments.output = path.resolve(argv[++index]);
        } else {
            throw new Error(`Unknown argument: ${argument}`);
        }
    }
    if (!arguments.frontendRoot) {
        throw new Error("--frontend-root is required");
    }
    return arguments;
}

function gitSha(repository) {
    return childProcess
        .execFileSync("git", ["rev-parse", "HEAD"], {
            cwd: repository,
            encoding: "utf8",
        })
        .trim();
}

function requireCleanRepository(repository) {
    const status = childProcess.execFileSync("git", ["status", "--porcelain"], {
        cwd: repository,
        encoding: "utf8",
    });
    if (status.trim()) {
        throw new Error(
            `Frontend checkout has uncommitted changes: ${repository}`,
        );
    }
}

function readOperations(frontendRoot, client) {
    const fileName =
        client === "public"
            ? "CBioPortalAPI-docs.json"
            : "CBioPortalAPIInternal-docs.json";
    const documentPath = path.join(
        frontendRoot,
        "packages/cbioportal-ts-api-client/src/generated",
        fileName,
    );
    const document = JSON.parse(fs.readFileSync(documentPath, "utf8"));
    const operations = new Map();
    for (const [apiPath, pathItem] of Object.entries(document.paths)) {
        for (const [method, operation] of Object.entries(pathItem)) {
            if (!HTTP_METHODS.has(method.toLowerCase())) {
                continue;
            }
            if (!operation.operationId) {
                throw new Error(
                    `${fileName} has an operation without operationId: ${method} ${apiPath}`,
                );
            }
            if (operations.has(operation.operationId)) {
                throw new Error(
                    `${fileName} has duplicate operationId: ${operation.operationId}`,
                );
            }
            operations.set(operation.operationId, {
                client,
                httpMethod: method.toUpperCase(),
                path: apiPath,
            });
        }
    }
    return operations;
}

function sourceFiles(sourceRoot) {
    const files = [];
    function visit(directory) {
        for (const entry of fs.readdirSync(directory, {
            withFileTypes: true,
        })) {
            const entryPath = path.join(directory, entry.name);
            if (entry.isDirectory()) {
                visit(entryPath);
            } else if (
                /\.(?:js|jsx|ts|tsx)$/.test(entry.name) &&
                !entry.name.endsWith(".d.ts")
            ) {
                files.push(entryPath);
            }
        }
    }
    visit(sourceRoot);
    return files.sort();
}

function clientKind(moduleName) {
    if (!/cbioportal(?:Internal)?ClientInstance$/.test(moduleName)) {
        return null;
    }
    return moduleName.includes("Internal") ? "internal" : "public";
}

function isTestFile(relativePath) {
    return (
        /(?:^|\/)(?:__tests__|test|tests)(?:\/|$)/.test(relativePath) ||
        /\.(?:spec|test)\.(?:js|jsx|ts|tsx)$/.test(relativePath)
    );
}

function collectCalls(frontendRoot) {
    const sourceRoot = path.join(frontendRoot, "src");
    const calls = [];
    for (const filePath of sourceFiles(sourceRoot)) {
        const relativePath = path
            .relative(frontendRoot, filePath)
            .split(path.sep)
            .join("/");
        const sourceText = fs.readFileSync(filePath, "utf8");
        const sourceFile = ts.createSourceFile(
            filePath,
            sourceText,
            ts.ScriptTarget.Latest,
            true,
            filePath.endsWith("x") ? ts.ScriptKind.TSX : ts.ScriptKind.TS,
        );
        const clients = new Map();
        const getters = new Map();

        for (const statement of sourceFile.statements) {
            if (
                !ts.isImportDeclaration(statement) ||
                !ts.isStringLiteral(statement.moduleSpecifier)
            ) {
                continue;
            }
            const kind = clientKind(statement.moduleSpecifier.text);
            const importClause = statement.importClause;
            if (!kind || !importClause) {
                continue;
            }
            if (importClause.name) {
                clients.set(importClause.name.text, kind);
            }
            if (
                importClause.namedBindings &&
                ts.isNamedImports(importClause.namedBindings)
            ) {
                for (const element of importClause.namedBindings.elements) {
                    const importedName = (element.propertyName || element.name)
                        .text;
                    if (
                        importedName === "getClient" ||
                        importedName === "getInternalClient"
                    ) {
                        getters.set(element.name.text, kind);
                    }
                }
            }
        }

        function kindFromType(typeNode) {
            if (!typeNode) {
                return null;
            }
            const typeName = typeNode.getText(sourceFile);
            if (typeName === "CBioPortalAPIInternal") {
                return "internal";
            }
            if (typeName === "CBioPortalAPI") {
                return "public";
            }
            return null;
        }

        function visit(node, inheritedClients) {
            let scopedClients = inheritedClients;
            if (ts.isFunctionLike(node)) {
                scopedClients = new Map(inheritedClients);
                for (const parameter of node.parameters) {
                    if (!ts.isIdentifier(parameter.name)) {
                        continue;
                    }
                    const kind = kindFromType(parameter.type);
                    if (kind) {
                        scopedClients.set(parameter.name.text, kind);
                    } else {
                        scopedClients.delete(parameter.name.text);
                    }
                }
            } else if (ts.isBlock(node)) {
                scopedClients = new Map(inheritedClients);
            }

            if (ts.isVariableDeclaration(node) && ts.isIdentifier(node.name)) {
                const initializer = node.initializer;
                if (
                    initializer &&
                    ts.isCallExpression(initializer) &&
                    ts.isIdentifier(initializer.expression) &&
                    getters.has(initializer.expression.text)
                ) {
                    scopedClients.set(
                        node.name.text,
                        getters.get(initializer.expression.text),
                    );
                } else if (kindFromType(node.type)) {
                    scopedClients.set(node.name.text, kindFromType(node.type));
                } else {
                    scopedClients.delete(node.name.text);
                }
            }

            if (
                ts.isCallExpression(node) &&
                ts.isPropertyAccessExpression(node.expression)
            ) {
                const propertyAccess = node.expression;
                let kind = null;
                if (ts.isIdentifier(propertyAccess.expression)) {
                    kind =
                        scopedClients.get(propertyAccess.expression.text) ||
                        null;
                } else if (
                    ts.isCallExpression(propertyAccess.expression) &&
                    ts.isIdentifier(propertyAccess.expression.expression)
                ) {
                    kind =
                        getters.get(
                            propertyAccess.expression.expression.text,
                        ) || null;
                }
                if (kind && !NON_API_METHODS.has(propertyAccess.name.text)) {
                    const location = sourceFile.getLineAndCharacterOfPosition(
                        node.getStart(sourceFile),
                    );
                    calls.push({
                        client: kind,
                        methodName: propertyAccess.name.text.replace(
                            /WithHttpInfo$/,
                            "",
                        ),
                        usage: isTestFile(relativePath) ? "test" : "production",
                        callSite: `${relativePath}:${location.line + 1}`,
                    });
                }
            }
            if (
                ts.isCallExpression(node) &&
                ts.isElementAccessExpression(node.expression)
            ) {
                const target = node.expression.expression;
                if (ts.isIdentifier(target) && scopedClients.has(target.text)) {
                    const location = sourceFile.getLineAndCharacterOfPosition(
                        node.getStart(sourceFile),
                    );
                    throw new Error(
                        `Dynamic client operation is not inventory-safe at ${relativePath}:${location.line + 1}`,
                    );
                }
            }
            if (
                ts.isPropertyAccessExpression(node) &&
                ts.isIdentifier(node.expression) &&
                scopedClients.has(node.expression.text) &&
                !NON_API_METHODS.has(node.name.text) &&
                !(
                    ts.isCallExpression(node.parent) &&
                    node.parent.expression === node
                )
            ) {
                const location = sourceFile.getLineAndCharacterOfPosition(
                    node.getStart(sourceFile),
                );
                if (isTestFile(relativePath)) {
                    calls.push({
                        client: scopedClients.get(node.expression.text),
                        methodName: node.name.text.replace(/WithHttpInfo$/, ""),
                        usage: "test",
                        callSite: `${relativePath}:${location.line + 1}`,
                    });
                } else {
                    throw new Error(
                        `Detached client operation is not inventory-safe at ${relativePath}:${location.line + 1}`,
                    );
                }
            }
            ts.forEachChild(node, (child) => visit(child, scopedClients));
        }
        visit(sourceFile, clients);
    }
    return calls;
}

function tsvValue(value) {
    if (/[\t\r\n]/.test(value)) {
        throw new Error(`TSV value contains a control character: ${value}`);
    }
    return value;
}

function render(frontendRoot) {
    const actualSha = gitSha(frontendRoot);
    if (actualSha !== EXPECTED_FRONTEND_SHA) {
        throw new Error(
            `Expected frontend ${EXPECTED_FRONTEND_SHA}, found ${actualSha}`,
        );
    }
    requireCleanRepository(frontendRoot);
    const operationsByClient = {
        public: readOperations(frontendRoot, "public"),
        internal: readOperations(frontendRoot, "internal"),
    };
    const grouped = new Map();
    for (const call of collectCalls(frontendRoot)) {
        const operation = operationsByClient[call.client].get(call.methodName);
        if (!operation) {
            throw new Error(
                `Unknown ${call.client} client operation ${call.methodName} at ${call.callSite}`,
            );
        }
        const key = `${call.client}\0${call.methodName}`;
        if (!grouped.has(key)) {
            grouped.set(key, {
                operation,
                operationId: call.methodName,
                usages: new Set(),
                callSites: new Set(),
            });
        }
        grouped.get(key).usages.add(call.usage);
        grouped.get(key).callSites.add(call.callSite);
    }

    const rows = [...grouped.values()].sort((left, right) => {
        const leftKey = `${left.operation.client}\0${left.operationId}`;
        const rightKey = `${right.operation.client}\0${right.operationId}`;
        return leftKey < rightKey ? -1 : leftKey > rightKey ? 1 : 0;
    });
    const lines = [
        [
            "frontend_sha",
            "client",
            "operation_id",
            "http_method",
            "path",
            "usage",
            "call_sites",
        ].join("\t"),
    ];
    for (const row of rows) {
        const usage =
            row.usages.size === 2 ? "production+test" : [...row.usages][0];
        lines.push(
            [
                actualSha,
                row.operation.client,
                row.operationId,
                row.operation.httpMethod,
                row.operation.path,
                usage,
                [...row.callSites].sort().join(","),
            ]
                .map(tsvValue)
                .join("\t"),
        );
    }
    return `${lines.join("\n")}\n`;
}

function main() {
    const arguments = parseArguments(process.argv.slice(2));
    const rendered = render(arguments.frontendRoot);
    if (arguments.check) {
        const current = fs.existsSync(arguments.output)
            ? fs.readFileSync(arguments.output, "utf8")
            : "";
        if (current !== rendered) {
            process.stderr.write(
                `${path.relative(process.cwd(), arguments.output)} is stale\n`,
            );
            process.exitCode = 1;
        }
    } else {
        fs.mkdirSync(path.dirname(arguments.output), { recursive: true });
        fs.writeFileSync(arguments.output, rendered);
    }
}

main();
