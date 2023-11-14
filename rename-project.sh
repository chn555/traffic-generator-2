#!/usr/bin/env bash

read -rp "Enter the new project name in camelCase: " newName

echo "validating input"

if ! echo "${newName}" | grep -E '([a-z]{1,}[A-Z])|(^.+[A-Z]{1,}[a-z])'  &> /dev/null
then
  echo "input was detected as not camelCase, exiting"
  echo "in camelCase the first word starts with a lower case letter, and all next words starts with an upper case letter."

  exit 1
fi

echo "checking prerequisites"

if ! command -v rename &> /dev/null
then
    echo "rename could not be found, attempting to install with brew"
    brew install rename
fi

echo "parsing naming variations"

oldName="cookieCutter"
oldFlatCase="$(tr "A-Z" "a-z" <<< "${oldName}")"
oldPascalCase="$(tr "a-z" "A-Z" <<< "${oldName:0:1}")${oldName:1}"
oldSnakeCase=$(echo "${oldName}"  | sed 's/\([^A-Z]\)\([A-Z0-9]\)/\1_\2/g' \
                                  | sed 's/\([A-Z0-9]\)\([A-Z0-9]\)\([^A-Z]\)/\1_\2\3/g' \
                                  | tr '[:upper:]' '[:lower:]')
oldKebabCase=$(echo "${oldSnakeCase}"  | sed -r 's/_/-/g')


newFlatCase="$(tr "A-Z" "a-z" <<< "${newName}")"
newPascalCase="$(tr "a-z" "A-Z" <<< "${newName:0:1}")${newName:1}"
newSnakeCase=$(echo "${newName}"  | sed 's/\([^A-Z]\)\([A-Z0-9]\)/\1_\2/g' \
                                  | sed 's/\([A-Z0-9]\)\([A-Z0-9]\)\([^A-Z]\)/\1_\2\3/g' \
                                  | tr '[:upper:]' '[:lower:]')
newKebabCase=$(echo "${newSnakeCase}"  | sed -r 's/_/-/g')

echo
echo "camelCase = ${newName}"
echo "flatcase = ${newFlatCase}"
echo "PascalCase = ${newPascalCase}"
echo "snake_case = ${newSnakeCase}"
echo "kebab-case = ${newKebabCase}"
echo

echo "replacing occurrences in files"

grep -ilr --exclude="rename-project.sh" --exclude-dir=".git" --exclude-dir=".gradle" "${oldName}" . | xargs -I@  sed -i '' "s/${oldName}/${newName}/g" @

grep -ilr --exclude="rename-project.sh" --exclude-dir=".git" --exclude-dir=".gradle" "${oldFlatCase}" . | xargs -I@  sed -i '' "s/${oldFlatCase}/${newFlatCase}/g" @

grep -ilr --exclude="rename-project.sh" --exclude-dir=".git" --exclude-dir=".gradle" "${oldPascalCase}" . | xargs -I@  sed -i '' "s/${oldPascalCase}/${newPascalCase}/g" @

grep -ilr --exclude="rename-project.sh" --exclude-dir=".git" --exclude-dir=".gradle" "${oldKebabCase}" . | xargs -I@  sed -i '' "s/${oldKebabCase}/${newKebabCase}/g" @

grep -ilr --exclude="rename-project.sh" --exclude-dir=".git" --exclude-dir=".gradle" "${oldSnakeCase}" . | xargs -I@  sed -i '' "s/${oldSnakeCase}/${newSnakeCase}/g" @

echo "renaming files and directories"

export LC_CTYPE=en_US.UTF-8
export LC_ALL=en_US.UTF-8

find . -exec rename  -S "${oldName}" "${newName}" {} +
find . -exec rename  -S "${oldFlatCase}" "${newFlatCase}" {} +
find . -exec rename  -S "${oldPascalCase}" "${newPascalCase}" {} +
find . -exec rename  -S "${oldKebabCase}" "${newKebabCase}" {} +
find . -exec rename  -S "${oldSnakeCase}" "${newSnakeCase}" {} +


echo "Done!"
echo "you might get 'already exists' errors for files in folders that contain the project name, that is OK"
echo "Your IDE might freak out until you refresh the gradle cache"