#!/usr/bin/env python3
"""
Rebuilds AstraDonutShop.jar from GGDonutShop.jar.

This is the exact driver used to produce AstraDonutShop.jar in this
repository. It:

  1. Extracts GGDonutShop.jar.
  2. Runs classpatch.py's constant-pool rewriter over every .class file
     (renaming packages/classes and every branded string literal, while
     leaving all executable bytecode byte-for-byte untouched).
  3. Renames files/directories using the same replacement rules.
  4. Rewrites plugin.yml, the YAML resource file headers, MANIFEST.MF,
     and the embedded Maven metadata with Astra/AstraLab/DevSolentz
     branding, and drops the GGPlugins hard dependency (Vault is the
     only remaining hard dependency).
  5. Re-packages everything into AstraDonutShop.jar.

Usage (from the repo root):
    python3 tools/rebrand_jar.py
"""
import os
import shutil
import sys
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
REPO_ROOT = os.path.dirname(HERE)
SRC_JAR = os.path.join(REPO_ROOT, "GGDonutShop.jar")
OUT_JAR = os.path.join(REPO_ROOT, "AstraDonutShop.jar")
WORK = os.path.join(REPO_ROOT, ".rebrand_work")
EXTRACT_DIR = os.path.join(WORK, "extracted")
BUILD_DIR = os.path.join(WORK, "build")

sys.path.insert(0, HERE)
from classpatch import patch_class_bytes, apply_replacements, REPLACEMENTS  # noqa: E402

PLUGIN_YML = """name: AstraDonutShop
version: 1.21.11-26.2
main: net.astralab.astradonutshop.AstraDonutShop
api-version: '1.21'
author: DevSolentz
description: AstraDonutShop by AstraLab - A modern, dynamic donut shop system for Minecraft Paper 1.21.11 with live dialogues, a randomizer-driven economy, and full Vault integration.
website: https://astralab.dev
depend:
  - Vault
softdepend:
  - Essentials
  - EssentialsX
commands:
  shop:
    description: Open the AstraDonutShop storefront
    usage: /shop
    aliases: [donutshop, astrashop]
  sell:
    description: Open the AstraDonutShop sell chest GUI to sell items
    usage: /sell
permissions:
  astradonutshop.admin:
    description: Grants access to AstraDonutShop admin actions (reload, price randomizer)
    default: op
"""

MANIFEST_MF = """Manifest-Version: 1.0
Created-By: AstraLab Build Tools
Implementation-Title: AstraDonutShop
Implementation-Version: 1.21.11-26.2
Implementation-Vendor: AstraLab
"""

POM_PROPERTIES = """artifactId=AstraDonutShop
groupId=net.astralab
version=1.21.11-26.2
"""

POM_XML = """<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>net.astralab</groupId>
    <artifactId>AstraDonutShop</artifactId>
    <version>1.21.11-26.2</version>
    <packaging>jar</packaging>

    <name>AstraDonutShop</name>
    <description>AstraDonutShop by AstraLab - Paper 1.21.11 dynamic donut shop plugin with modern dialogues and Vault economy integration.</description>

    <properties>
        <java.version>21</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <repositories>
        <repository>
            <id>papermc</id>
            <url>https://repo.papermc.io/repository/maven-public/</url>
        </repository>
        <repository>
            <id>essentialsx-releases</id>
            <url>https://repo.essentialsx.net/releases/</url>
        </repository>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>io.papermc.paper</groupId>
            <artifactId>paper-api</artifactId>
            <version>1.21.11-R0.1-SNAPSHOT</version>
            <scope>system</scope>
            <systemPath>${project.basedir}/libs/paper-api-1.21.11.jar</systemPath>
        </dependency>
        <dependency>
            <groupId>net.kyori</groupId>
            <artifactId>adventure-api</artifactId>
            <version>4.20.0-SNAPSHOT</version>
            <scope>system</scope>
            <systemPath>${project.basedir}/libs/adventure-api.jar</systemPath>
        </dependency>
        <dependency>
            <groupId>net.kyori</groupId>
            <artifactId>adventure-key</artifactId>
            <version>4.20.0-SNAPSHOT</version>
            <scope>system</scope>
            <systemPath>${project.basedir}/libs/adventure-key.jar</systemPath>
        </dependency>
        <dependency>
            <groupId>net.kyori</groupId>
            <artifactId>adventure-nbt</artifactId>
            <version>4.20.0-SNAPSHOT</version>
            <scope>system</scope>
            <systemPath>${project.basedir}/libs/adventure-nbt.jar</systemPath>
        </dependency>
        <dependency>
            <groupId>net.kyori</groupId>
            <artifactId>adventure-text-minimessage</artifactId>
            <version>4.20.0-SNAPSHOT</version>
            <scope>system</scope>
            <systemPath>${project.basedir}/libs/adventure-text-minimessage.jar</systemPath>
        </dependency>
        <dependency>
            <groupId>net.kyori</groupId>
            <artifactId>examination-api</artifactId>
            <version>1.3.0</version>
            <scope>system</scope>
            <systemPath>${project.basedir}/libs/examination-api.jar</systemPath>
        </dependency>
        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <version>32.1.2-jre</version>
            <scope>system</scope>
            <systemPath>${project.basedir}/libs/guava.jar</systemPath>
        </dependency>
        <dependency>
            <groupId>org.jetbrains</groupId>
            <artifactId>annotations</artifactId>
            <version>24.1.0</version>
            <scope>system</scope>
            <systemPath>${project.basedir}/libs/annotations.jar</systemPath>
        </dependency>
        <dependency>
            <groupId>net.md_5</groupId>
            <artifactId>bungeecord-chat</artifactId>
            <version>1.20-R0.2</version>
            <scope>system</scope>
            <systemPath>${project.basedir}/libs/bungeecord-chat.jar</systemPath>
        </dependency>
        <dependency>
            <groupId>com.github.MilkBowl</groupId>
            <artifactId>VaultAPI</artifactId>
            <version>1.7</version>
            <scope>system</scope>
            <systemPath>${project.basedir}/libs/VaultAPI-1.7.1.jar</systemPath>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.13.0</version>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-resources-plugin</artifactId>
                <version>3.3.1</version>
                <configuration>
                    <encoding>UTF-8</encoding>
                </configuration>
            </plugin>
        </plugins>
        <resources>
            <resource>
                <directory>src/main/resources</directory>
                <filtering>true</filtering>
            </resource>
        </resources>
    </build>
</project>
"""


def rewrite_text_file(path, old, new):
    with open(path, "rb") as f:
        data = f.read()
    assert old.encode() in data, f"expected header not found in {path}"
    data = data.replace(old.encode(), new.encode())
    with open(path, "wb") as f:
        f.write(data)


def main():
    if os.path.exists(WORK):
        shutil.rmtree(WORK)
    os.makedirs(EXTRACT_DIR)
    os.makedirs(BUILD_DIR)

    with zipfile.ZipFile(SRC_JAR) as z:
        z.extractall(EXTRACT_DIR)

    total_changed = 0
    for root, _dirs, files in os.walk(EXTRACT_DIR):
        for fn in files:
            src_path = os.path.join(root, fn)
            rel = os.path.relpath(src_path, EXTRACT_DIR)
            new_rel = apply_replacements(rel.encode()).decode()

            if fn.endswith(".class"):
                with open(src_path, "rb") as f:
                    buf = f.read()
                new_buf, changed = patch_class_bytes(buf, REPLACEMENTS)
                total_changed += changed
                dst_path = os.path.join(BUILD_DIR, new_rel)
                os.makedirs(os.path.dirname(dst_path), exist_ok=True)
                with open(dst_path, "wb") as f:
                    f.write(new_buf)
            else:
                dst_path = os.path.join(BUILD_DIR, new_rel)
                os.makedirs(os.path.dirname(dst_path), exist_ok=True)
                shutil.copyfile(src_path, dst_path)

    print(f"Rewrote {total_changed} branded constant-pool entries across all classes")

    # Drop the old Maven metadata directory (net.notdakuxd/GGDonutShop)
    # and replace it with Astra-branded metadata.
    old_maven_dir = os.path.join(BUILD_DIR, "META-INF", "maven", "net.notdakuxd")
    if os.path.isdir(old_maven_dir):
        shutil.rmtree(old_maven_dir)
    new_maven_dir = os.path.join(BUILD_DIR, "META-INF", "maven", "net.astralab", "AstraDonutShop")
    os.makedirs(new_maven_dir, exist_ok=True)
    with open(os.path.join(new_maven_dir, "pom.properties"), "w") as f:
        f.write(POM_PROPERTIES)
    with open(os.path.join(new_maven_dir, "pom.xml"), "w") as f:
        f.write(POM_XML)
    with open(os.path.join(BUILD_DIR, "META-INF", "MANIFEST.MF"), "w") as f:
        f.write(MANIFEST_MF)

    # plugin.yml: full rewrite (removes the GGPlugins hard dependency;
    # Vault becomes the only hard dependency).
    with open(os.path.join(BUILD_DIR, "plugin.yml"), "w") as f:
        f.write(PLUGIN_YML)

    # Resource file header comments.
    rewrite_text_file(
        os.path.join(BUILD_DIR, "config.yml"),
        "# ============================================================\n"
        "#               NxDonutShop Configuration\n"
        "#          Paper 1.21.11 Modern Dialogues & Economy Shop\n"
        "# ============================================================",
        "# ============================================================\n"
        "#               AstraDonutShop Configuration\n"
        "#          Paper 1.21.11 Modern Dialogues & Economy Shop\n"
        "#                    Developed by AstraLab\n"
        "# ============================================================",
    )
    rewrite_text_file(
        os.path.join(BUILD_DIR, "shop.yml"),
        "# =====================================================\r\n"
        "#           NxDonutShop-Modern - Complete shop.yml\r\n"
        "#           Author: NotDaKuxD | Paper 1.21.11\r\n"
        "# =====================================================\r\n",
        "# =====================================================\r\n"
        "#           AstraDonutShop-Modern - Complete shop.yml\r\n"
        "#           AstraLab | Author: DevSolentz | Paper 1.21.11\r\n"
        "# =====================================================\r\n",
    )
    rewrite_text_file(
        os.path.join(BUILD_DIR, "enchantment.yml"),
        "# =======================================================\n"
        "#               NxDonutShop - Enchantment Pricing\n"
        "# =======================================================",
        "# =======================================================\n"
        "#          AstraDonutShop - Enchantment Pricing (AstraLab)\n"
        "# =======================================================",
    )

    # Repackage.
    files = []
    for root, _dirs, filenames in os.walk(BUILD_DIR):
        for fn in filenames:
            full = os.path.join(root, fn)
            rel = os.path.relpath(full, BUILD_DIR)
            files.append(rel)
    files.sort(key=lambda p: (p != "META-INF/MANIFEST.MF", p))

    if os.path.exists(OUT_JAR):
        os.remove(OUT_JAR)
    with zipfile.ZipFile(OUT_JAR, "w", zipfile.ZIP_DEFLATED) as z:
        for rel in files:
            z.write(os.path.join(BUILD_DIR, rel), rel)

    shutil.rmtree(WORK)
    print(f"Wrote {OUT_JAR} ({len(files)} entries)")


if __name__ == "__main__":
    main()
