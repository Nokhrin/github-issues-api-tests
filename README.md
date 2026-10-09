# Автоматизация тестирования REST API

# Цель

Тестирование сервиса `GitHub Issues`

# Подготовка

создан репо https://github.com/Nokhrin/api-testing-lab/
созданы токены, записаны в .env в корне репозитория
как
READ_AND_WRITE_TOKEN - сценарии создания, обновления, комментирования, закрытия issues, чтение репо, issue
READ_TOKEN - сценарии "ошибка доступа" при изменении

# GitHub API

[Документация](https://docs.github.com/en/rest/using-the-rest-api/getting-started-with-the-rest-api?spm=a2ty_o01.29997173.0.0.50ad55fbEykyRG&apiVersion=2026-03-10)

[Спецификация OpenAPI](https://raw.githubusercontent.com/github/rest-api-description/refs/heads/main/descriptions/api.github.com/api.github.com.json)

Поиск в спецификации
```shell
curl -s https://raw.githubusercontent.com/github/rest-api-description/refs/heads/main/descriptions/api.github.com/api.github.com.json | jq '.paths["/repos/{owner}/{repo}/issues"].post.requestBody.content."application/json".schema'
```

# Разработка

## Конфигурация

```shell
# 1. Создать .env
touch .env
chmod 600 .env
# 2. Заполнить .env
# 3. Загрузить переменные
set -a; source .env; set +a;
```

## Выполнение тестов
### По группам
```shell
mvn -B -ntp test -Dgroups=read
```


## Чистка окружения

### Закрыть открытые issues
```shell
mvn test-compile exec:java
```

## Отчетность
Один тест с отчетом
```shell
mvn clean test allure:report -Dtest=org.nokhrin.github.graphql.IssueTest#verifyIssuesQueryReturnsValidIssues
```


```shell
# Запустить тесты и сгенерировать отчёт
mvn clean test allure:report

# В произвольном каталоге
mvn -Dallure.results.directory=path/to/allure-results allure:serve
mvn -Dallure.results.directory=/tmp/allure allure:serve
# 

# Открыть отчёт в браузере
firefox target/site/allure-maven-plugin/index.html
```

## Взаимодействие по протоколам
Пример (REST): GET /repos/{owner}/{repo}/issues -> HTTP 200, массив объектов JSON
Пример (GraphQL): POST /graphql -> HTTP 200, объект JSON с обязательным ключом data (или errors)
