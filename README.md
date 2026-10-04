# Автоматизация тестирования REST API

# Цель

Тестирование сервиса `GitHub Issues`

# Подготовка

создан репо https://github.com/Nokhrin/api-testing-lab/
созданы токены, записаны в .env в корне репозитория
как
READ_AND_WRITE_TOKEN - сценарии создания, обновления, комментирования, закрытия issues, чтение репо, issue
READ_TOKEN - сценарии "ошибка доступа" при изменении

переменные в корне проекта в файле `.env`

создан .gitignore с исключением `.env`

# План

- заглушки
- авторизация и права доступа
- GET /zen - проверка доступности.
- GET /repos/{owner}/{repo} - проверка контракта репозитория.
- GET /repos/{owner}/{repo}/issues - проверка списка.
- POST /repos/{owner}/{repo}/issues - создание issue.
- GET /repos/{owner}/{repo}/issues/{issue_number} - получение созданного issue.
- PATCH /repos/{owner}/{repo}/issues/{issue_number} - обновление issue.
- POST /repos/{owner}/{repo}/issues/{issue_number}/comments - создание комментария.
- GET /repos/{owner}/{repo}/issues/{issue_number}/comments - получение комментариев.
- PATCH закрытия issue.
- GET закрытого issue.
- Создание issue без токена - ожидание ошибки.
- Запрос без User-Agent - ожидание ошибки.
- Несуществующий issue - 404.
- Несуществующий репозиторий - 404.
- Пустой или невалидный title - 422.
- Невалидный JSON - 400.
- Фильтрация по state.
- Пагинация через per_page.
- Проверка заголовков X-RateLimit-*.
- Проверка структуры ошибки.

# GitHub API

[Документация](https://docs.github.com/en/rest/using-the-rest-api/getting-started-with-the-rest-api?spm=a2ty_o01.29997173.0.0.50ad55fbEykyRG&apiVersion=2026-03-10)

# Разработка

## Конфигурация

```shell
# 1. Создать .env
touch .env
chmod 600 .env
# 2. Заполнить .env
# 3. Загрузить переменные и запустить тесты
set -a; source .env; set +a;
mvn test
```

## Чистка окружения

Закрыть открытые issues

```shell
mvn test -Dcleanup=true -Dtest=CleanupRunner
```
