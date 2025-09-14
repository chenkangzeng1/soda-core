#!/bin/bash

# =============================
# 用户配置部分
# =============================
GROUP_ID="com.hibuka.soda"
ARTIFACT_ID="soda-core"

# =============================
# 获取项目根目录
# =============================
PROJECT_ROOT=$(pwd)
WORK_DIR="${PROJECT_ROOT}/.zip-workspace"

# =============================
# 提取版本号（从 pom.xml）
# =============================
# 首先尝试从 Maven 获取实际版本号
if command -v mvn >/dev/null 2>&1; then
  echo "尝试使用 Maven 获取版本号..."
  VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout 2>/dev/null)
  if [ -z "$VERSION" ] || [ "$VERSION" = "null" ] || [[ "$VERSION" == *"\${"* ]]; then
    echo "Maven 命令失败或返回变量引用，尝试备用方案..."
    VERSION=""
  fi
fi

# 如果 Maven 失败，尝试从 pom.xml 中直接提取
if [ -z "$VERSION" ]; then
  echo "从 pom.xml 直接提取版本号..."
  VERSION=$(grep -m1 '<version>' "${PROJECT_ROOT}/pom.xml" | sed -E 's/.*<version>([^<]+)<\/version>.*/\1/')
  
  # 如果提取到的是变量引用，尝试从 profiles 中获取
  if [[ "$VERSION" == *"\${"* ]]; then
    echo "检测到变量引用，尝试从 profiles 中提取..."
    # 查找第一个包含 project.version 的 profile
    PROFILE_VERSION=$(grep -A 20 '<profile>' "${PROJECT_ROOT}/pom.xml" | grep -m1 '<project.version>' | sed -E 's/.*<project.version>([^<]+)<\/project.version>.*/\1/')
    if [ -n "$PROFILE_VERSION" ]; then
      VERSION="$PROFILE_VERSION"
      echo "从 profile 中提取到版本号: ${VERSION}"
    else
      echo "无法从 pom.xml 中提取有效版本号"
      exit 1
    fi
  fi
fi

# 移除 -SNAPSHOT 后缀（如果存在）
VERSION=${VERSION%-SNAPSHOT}
echo "处理后的版本号: ${VERSION}"
echo "最终版本号: ${VERSION}"

# 验证版本号是否有效
if [ -z "$VERSION" ] || [[ "$VERSION" == *"\${"* ]]; then
  echo "版本号提取失败，请检查 pom.xml 配置"
  exit 1
fi

MODULE_PATH="${WORK_DIR}/com/trieai/scoda/${ARTIFACT_ID}/${VERSION}"
ZIP_FILE="${ARTIFACT_ID}-${VERSION}-upload.zip"
ZIP_FULL_PATH="${PROJECT_ROOT}/${ZIP_FILE}"

echo "模块路径: ${MODULE_PATH}"
echo "ZIP 文件: ${ZIP_FILE}"

# =============================
# 清理旧数据
# =============================
rm -rf "${WORK_DIR}" "${ZIP_FULL_PATH}"
mkdir -p "${MODULE_PATH}"

# 验证目录是否创建成功
if [ ! -d "${MODULE_PATH}" ]; then
  echo "无法创建模块目录: ${MODULE_PATH}"
  exit 1
fi
echo "模块目录创建成功: ${MODULE_PATH}"

# =============================
# 检查 target 目录是否存在
# =============================
if [ ! -d "target" ]; then
  echo "target/ 目录不存在，请先运行 mvn clean package source:jar javadoc:jar"
  exit 1
fi

# =============================
# 创建 .pom 文件（如果不存在）
# =============================
POM_TARGET="${PROJECT_ROOT}/target/${ARTIFACT_ID}-${VERSION}.pom"
if [ ! -f "${POM_TARGET}" ]; then
  echo "未找到 .pom 文件，正在从 pom.xml 生成..."
  cp pom.xml "${POM_TARGET}"
fi

# =============================
# 复制构建产物到目标目录
# =============================
echo "正在复制构建产物..."

# 复制主JAR文件
if [ -f "target/${ARTIFACT_ID}-${VERSION}.jar" ]; then
  cp "target/${ARTIFACT_ID}-${VERSION}.jar" "${MODULE_PATH}/"
  echo "已复制主JAR文件: ${ARTIFACT_ID}-${VERSION}.jar"
elif [ -f "target/${ARTIFACT_ID}-${VERSION}-SNAPSHOT.jar" ]; then
  cp "target/${ARTIFACT_ID}-${VERSION}-SNAPSHOT.jar" "${MODULE_PATH}/${ARTIFACT_ID}-${VERSION}.jar"
  echo "已复制主JAR文件: ${ARTIFACT_ID}-${VERSION}-SNAPSHOT.jar -> ${ARTIFACT_ID}-${VERSION}.jar"
else
  echo "错误: 未找到主JAR文件: ${ARTIFACT_ID}-${VERSION}.jar 或 ${ARTIFACT_ID}-${VERSION}-SNAPSHOT.jar"
  exit 1
fi

# 复制sources JAR文件
if [ -f "target/${ARTIFACT_ID}-${VERSION}-sources.jar" ]; then
  cp "target/${ARTIFACT_ID}-${VERSION}-sources.jar" "${MODULE_PATH}/"
  echo "已复制sources JAR文件: ${ARTIFACT_ID}-${VERSION}-sources.jar"
elif [ -f "target/${ARTIFACT_ID}-${VERSION}-SNAPSHOT-sources.jar" ]; then
  cp "target/${ARTIFACT_ID}-${VERSION}-SNAPSHOT-sources.jar" "${MODULE_PATH}/${ARTIFACT_ID}-${VERSION}-sources.jar"
  echo "已复制sources JAR文件: ${ARTIFACT_ID}-${VERSION}-SNAPSHOT-sources.jar -> ${ARTIFACT_ID}-${VERSION}-sources.jar"
else
  echo "错误: 未找到sources JAR文件: ${ARTIFACT_ID}-${VERSION}-sources.jar 或 ${ARTIFACT_ID}-${VERSION}-SNAPSHOT-sources.jar"
  echo "请运行: mvn clean package source:jar javadoc:jar"
  exit 1
fi

# 复制javadoc JAR文件
if [ -f "target/${ARTIFACT_ID}-${VERSION}-javadoc.jar" ]; then
  cp "target/${ARTIFACT_ID}-${VERSION}-javadoc.jar" "${MODULE_PATH}/"
  echo "已复制javadoc JAR文件: ${ARTIFACT_ID}-${VERSION}-javadoc.jar"
elif [ -f "target/${ARTIFACT_ID}-${VERSION}-SNAPSHOT-javadoc.jar" ]; then
  cp "target/${ARTIFACT_ID}-${VERSION}-SNAPSHOT-javadoc.jar" "${MODULE_PATH}/${ARTIFACT_ID}-${VERSION}-javadoc.jar"
  echo "已复制javadoc JAR文件: ${ARTIFACT_ID}-${VERSION}-SNAPSHOT-javadoc.jar -> ${ARTIFACT_ID}-${VERSION}-javadoc.jar"
else
  echo "错误: 未找到javadoc JAR文件: ${ARTIFACT_ID}-${VERSION}-javadoc.jar 或 ${ARTIFACT_ID}-${VERSION}-SNAPSHOT-javadoc.jar"
  echo "请运行: mvn clean package source:jar javadoc:jar"
  exit 1
fi

# 生成POM文件（从pom.xml复制并替换版本号）
if [ -f "pom.xml" ]; then
  sed "s/\${project.version}-SNAPSHOT/${VERSION}/g" pom.xml > "${MODULE_PATH}/${ARTIFACT_ID}-${VERSION}.pom"
  echo "已生成POM文件: ${ARTIFACT_ID}-${VERSION}.pom"
else
  echo "错误: 未找到pom.xml文件"
  exit 1
fi

# =============================
# 签名所有构件
# =============================
cd "${MODULE_PATH}" || {
  echo "进入模块目录失败"
  exit 1
}

for file in *.jar *.pom; do
    if [ -f "$file" ]; then
        echo "正在对 $file 签名..."
        gpg --armor --detach-sign "$file" || {
          echo "GPG 签名失败，请确认你已配置好 GPG 密钥"
          exit 1
        }
    fi
done

# =============================
# 生成校验文件（MD5 / SHA1）
# =============================
cd "${MODULE_PATH}" || {
  echo "回到模块目录失败"
  exit 1
}

if command -v md5sum >/dev/null 2>&1; then
  for file in *; do
    if [ -f "$file" ] && [[ ! "$file" == *.md5 ]] && [[ ! "$file" == *.sha1 ]]; then
      md5sum "$file" | awk '{print $1}' > "$file.md5"
    fi
  done
fi

if command -v sha1sum >/dev/null 2>&1; then
  for file in *; do
    if [ -f "$file" ] && [[ ! "$file" == *.md5 ]] && [[ ! "$file" == *.sha1 ]]; then
      sha1sum "$file" | awk '{print $1}' > "$file.sha1"
    fi
  done
fi

# =============================
# 回到项目根目录并打包 ZIP
# =============================
cd "${PROJECT_ROOT}" || {
  echo "回到项目根目录失败"
  exit 1
}

echo "正在打包 ZIP 文件..."
(cd "${WORK_DIR}" && zip -r "${ZIP_FULL_PATH}" com)

# =============================
# 验证 ZIP 是否存在并查看内容
# =============================
if [ ! -f "${ZIP_FULL_PATH}" ]; then
  echo "ZIP 文件未生成，请检查路径或权限"
  exit 1
fi

echo "ZIP 文件已生成：${ZIP_FULL_PATH}"
echo "ZIP 内容如下："
unzip -l "${ZIP_FULL_PATH}" | head -n 30

# =============================
# 可选清理工作目录
# =============================
rm -rf "${WORK_DIR}"

echo ""
echo "ZIP 准备完成！可以上传至 Maven Central"
