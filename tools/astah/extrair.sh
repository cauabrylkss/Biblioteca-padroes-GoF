#!/usr/bin/env bash
# Exporta PNGs e o modelo textual de todos os .asta de docs/ usando o Astah instalado.
# Uso (Git Bash, na raiz do repo):  bash tools/astah/extrair.sh [pasta_saida]
# Requer Astah UML em "C:/Program Files/astah-UML" (traz um JDK 25 com compilador embutido).
set -euo pipefail
ASTAH="C:/Program Files/astah-UML"
JAVA="$ASTAH/jre/bin/java.exe"
CP="$ASTAH/astah-api.jar;$ASTAH/astah-uml.jar"
OUT="${1:-build/astah}"
mkdir -p "$OUT"
for f in docs/*.asta; do
  n=$(basename "$f" .asta)
  mkdir -p "$OUT/$n"
  # PNG de cada diagrama
  "$JAVA" -Xmx1024m -cp "$ASTAH/astah-uml.jar" com.change_vision.jude.cmdline.JudeCommandRunner \
    -image all -f "$(cygpath -w "$f")" -t png -o "$OUT/$n" 2>&1 | grep -v -E "\[cds\]|<INFO>" || true
  # Modelo textual (classes, operações, transições, mensagens...)
  "$JAVA" -cp "$CP" tools/astah/DumpModelo.java "$(cygpath -w "$f")" 2>&1 | grep -v -E "\[cds\]|<INFO>" > "$OUT/$n.txt" || true
  echo "$n -> $OUT/$n.txt"
done
