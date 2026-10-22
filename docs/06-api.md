# API

Спецификация OpenAPI лежит в `backend/src/main/resources/static/api/openapi.yaml`. Она написана вручную, а интеграционный тест `ApiIntegrationTest.openApiContractInSync` проверяет, что все эндпоинты из кода в ней есть.

Swagger: http://localhost:3000/api/docs. Чтобы вызывать методы, нужно получить токен через `/api/auth/login` и вставить его в Authorize.

Базовый адрес `http://localhost:3000/api`, в Docker — `http://localhost:8080/api`. Суммы в рублях, можно числом `1500.5` или строкой `"1500,50"`.

## Авторизация

Регистрация и вход возвращают JWT. Дальше его нужно передавать в заголовке `Authorization: Bearer <token>`. Токен живёт 24 часа. Без токена доступны только вход, регистрация, `/api/health` и Swagger.

## Эндпоинты

| Метод | URL | Роль | Что делает |
|---|---|---|---|
| POST | `/api/auth/register` | все | Регистрация |
| POST | `/api/auth/login` | все | Вход |
| GET | `/api/auth/me` | любая | Текущий пользователь |
| GET | `/api/accounts` | CLIENT | Мои счета |
| POST | `/api/accounts` | CLIENT | Открыть счёт |
| GET | `/api/accounts/{id}` | CLIENT | Счёт и остаток дневного лимита |
| POST | `/api/transfers` | CLIENT | Перевод |
| GET | `/api/operations` | CLIENT | История, можно фильтровать по `accountId` |
| GET | `/api/cards` | CLIENT | Мои карты |
| POST | `/api/cards` | CLIENT | Выпустить карту |
| POST | `/api/cards/{id}/block`, `/unblock` | CLIENT | Заблокировать или разблокировать карту |
| GET | `/api/categories` | любая | Категории оплаты |
| POST | `/api/payments` | CLIENT | Оплата услуги |
| GET | `/api/stats/spending` | CLIENT | Расходы по категориям за месяц |
| GET | `/api/autopayments` | CLIENT | Мои автоплатежи |
| POST | `/api/autopayments` | CLIENT | Создать автоплатёж |
| POST | `/api/autopayments/{id}/disable` | CLIENT | Отключить автоплатёж |
| GET | `/api/admin/users` | ADMIN | Пользователи |
| POST | `/api/admin/users/{id}/block`, `/unblock` | ADMIN | Заблокировать или разблокировать пользователя |
| GET | `/api/admin/operations` | ADMIN | Все операции |
| POST | `/api/admin/autopayments/run` | ADMIN | Запустить автоплатежи |
| GET | `/api/health` | все | Проверка, что сервер работает |
| POST | `/api/test/reset` | все | Сброс базы, только в профиле test |
| GET, POST | `/api/test/clock` | все | Сдвиг времени сервера, только в профиле test |

## Пример

```http
POST /api/transfers
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{ "fromAccountId": 1, "toAccountId": 2, "amount": "250", "comment": "На отпуск" }
```

Ответ `201 Created` содержит операцию и оба счёта с новыми остатками.

## Ошибки

Все ошибки приходят в одном формате:

```json
{ "error": { "code": "INSUFFICIENT_FUNDS", "message": "Недостаточно средств на счёте списания", "details": { "balance": 100, "amount": 100.01 } } }
```

| HTTP | Код | Когда |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Не хватает поля или оно заполнено неверно |
| 400 | `INVALID_AMOUNT` | Сумма 0, отрицательная, не число или больше двух знаков после запятой |
| 400 | `INVALID_JSON` | Тело запроса не JSON |
| 401 | `UNAUTHORIZED` | Нет токена или он недействителен |
| 401 | `INVALID_CREDENTIALS` | Неверный email или пароль |
| 403 | `FORBIDDEN` | Клиент обращается к админскому API |
| 403 | `USER_BLOCKED` | Пользователь заблокирован |
| 404 | `ACCOUNT_NOT_FOUND`, `CARD_NOT_FOUND`, `AUTOPAYMENT_NOT_FOUND` | Нет такого объекта или он чужой |
| 404 | `CATEGORY_NOT_FOUND`, `USER_NOT_FOUND`, `NOT_FOUND` | Нет категории, пользователя или такого адреса |
| 409 | `EMAIL_TAKEN` | Email уже занят |
| 422 | `SAME_ACCOUNT` | Перевод на тот же счёт |
| 422 | `INSUFFICIENT_FUNDS` | Не хватает денег |
| 422 | `DAILY_LIMIT_EXCEEDED` | Превышен дневной лимит переводов |
| 422 | `CARD_BLOCKED`, `CARD_EXPIRED` | Карта заблокирована или просрочена |
| 422 | `CANNOT_BLOCK_ADMIN` | Попытка заблокировать администратора |
| 500 | `INTERNAL_ERROR` | Непредвиденная ошибка |

Почему так:
- 401 значит, что сервер не знает, кто вы, и нужно войти заново. 403 значит, что знает, но нельзя.
- На чужие объекты API отвечает 404, а не 403, чтобы перебором id нельзя было узнать, какие счета существуют.
- 422 — запрос правильный по формату, но нарушает бизнес-правило. 400 — ошибка в самом запросе.
