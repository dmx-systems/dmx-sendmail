
> The `dm4-sendgrid` GitHub repo is a mirror repo. The master repo is `dmx-sendmail`, hosted at the DMX company's [GitLab](https://git.dmx.systems/dmx-plugins/dmx-sendmail). DeepaMehta was rebranded as DMX. DeepaMehta 5 and DMX is the same; same code; same features; similar free software license (AGPL-3.0 vs GPL-3.0). The master branch represents DeepaMehta 5. For DeepaMehta 4 switch to the [dm4 branch](/tree/dm4).

## DMX Sendmail

This DMX Plugin can be used to send e-mails via SMTP or via [Sendgrid Web API v3](https://sendgrid.com/docs/API_Reference/Web_API_v3/index.html). It's core feature is a service which allows other plugins to send emails to the `SYSTEM_ADMIN_MAILBOX` or to one or many other recipients with the sender being `SYSTEM_FROM_NAME` and `SYSTEM_FROM_MAILBOX`.

## Configuration

To configure the dmx-sendmail plugin you can set the following system properties, e.g. trough adding them to the `config.properties` file of your DMX installation (`dmx-platform/conf/config.properties`). When running dmx-platform from sources configuration needs to be done in the platforms `pom.xml`.

The plugins default configuration is represented by the values before the `|` in the following section:

```
## Sendmail Plugin ## 
dmx.sendmail.system_admin_mailbox = root@dlocalhost | admin@domain.tld
dmx.sendmail.system_from_mailbox = dmx@localhost | your@system.com
dmx.sendmail.system_from_name = DMX Sendmail | Your System Name
dmx.sendmail.type = smtp | sendgrid
dmx.sendmail.greeting_enabled = false | true
#dmx.sendmail.greeting_subject = <The subject for the greeting email>
#dmx.sendmail.greeting_message = <The message for the greeting email>
#dmx.sendmail.greeting_html_message = <The HTML message for the greeting email>

## Sendmail SMTP Configuration
dmx.sendmail.smtp_host = localhost | ip/hostname
dmx.sendmail.smtp_username = empty | username
dmx.sendmail.smtp_password = empty | password
dmx.sendmail.smtp_port = 25 | port
dmx.sendmail.smtp_security = empty | tls | smtps
dmx.sendmail.smtp_debug = true | false

## Sendmail Sendgrid Configuration
dmx.sendmail.sendgrid_api_key = empty
```

## License

DMX Sendmail software is available freely under the GNU Affero General Public License, version 3.

All third party components incorporated into the DMX Sendmail Software are licensed under the original license provided by the owner of the applicable component.

- JavaMail API (compat) 1.4.5, [CDDL 1.0](https://javaee.github.io/javamail/LICENSE), Copyright (c) 2020 Oracle
- JavaBeans(TM) Activation Framework 1.1.1, [CDDL 1.0](https://spdx.org/licenses/CDDL-1.0.html), Copyright (c) 2020 Oracle
- Commons Email 1.3.2, [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0), Copyright (c) 2001-2018 The Apache Software Foundation. All Rights Reserved.
- Jsoup 1.7.2, [MIT License](https://jsoup.org/license), Copyright (c) 2009-2020 Jonathan Hedley (https://jsoup.org/)
- Commons IO, [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0), Copyright (c) 2002-2020 The Apache Software Foundation. All Rights Reserved.


## Release Notes

**2.1.0**, TBD

- API change: Plain text and HTML message body can be specified separately
- Add greeting enabled and message configuration option

**2.0.2**, Jun 30, 2021

- Compatible with DMX 5.2

**2.0.1**, Jan 03, 2021

- Upgraded dependencies to DMX 5.1
- Added copyright notices for dependencies

**2.0.0**, Dec 15, 2019

- Changed license from GPL to AGPL
- Renamed artifact to `dmx-sendmail`
- Adapted to DMX 5.0-beta-4+
- Support for sending mails via SMTP or Sendgrid

**1.2**, Aug 11, 2019

* Bugfix release for kiezatlas.berlin

**1.0**, Feb 12, 2017

* Allows to send simple HTML Mails (From and To) via Sendgrid

## Authors

Copyright (C) 2016-2018 Malte Reißig<br/>
Copyright (C) 2019-2020 DMX Systems
