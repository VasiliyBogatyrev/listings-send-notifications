## Description
This project sends notifications of new listings on zoopla.co.uk.
The URL from the site is used to search.

## Getting Started
For start server resolve in properties values:
* `telegram-bot.name`
* `telegram-bot.token`

To change the start period of scan, changes in the config `scanner.periodInSeq`, default value = 60 sec.
## Commands to Telegram Bot
* `/start <url>` add search by URL
* `/stop` stopped all active searching
