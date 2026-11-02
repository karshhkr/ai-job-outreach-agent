# AI Job Outreach Agent — backend MVP

A Java 21 + Spring Boot 4 + MySQL backend that imports HR contacts from Excel, stores your candidate profile/resume, generates personalized email drafts with OpenAI, and sends only after you explicitly approve them.

## Important scope

This is a runnable **backend MVP**, not a polished web dashboard. It uses Gmail SMTP with a Google App Password (not Gmail OAuth). It does not automatically scrape HR email addresses, bypass provider limits, or send emails without your approval.

## Requirements

- JDK 21
- Maven 3.9+ (or use IntelliJ's Maven tool window)
- MySQL 8.x running locally
- Internet access for Maven dependencies and OpenAI
- IntelliJ IDEA or Windows terminal
- An OpenAI API key for AI draft generation
- Gmail account with 2-Step Verification and a Google App Password for sending

## 1. Create database

In MySQL Workbench or MySQL CLI:

```sql
CREATE DATABASE outreach_agent_db;
```

## 2. Configure secrets (Windows CMD)

In the same CMD window where you will run the app, set:

```cmd
set DB_USERNAME=root
set DB_PASSWORD=YOUR_MYSQL_PASSWORD
set OPENAI_API_KEY=YOUR_OPENAI_API_KEY
set GMAIL_USERNAME=youraddress@gmail.com
set GMAIL_APP_PASSWORD=YOUR_16_CHARACTER_GOOGLE_APP_PASSWORD
```

Do not share these values or commit them. These `set` values apply only to the current CMD window. Restarting the terminal means setting them again.

If you want to run without AI/email initially, leave OPENAI_API_KEY and Gmail values unset. Health, profile, contacts, and Excel import still work.

## 3. Run

From the extracted project folder:

```cmd
.\mvnw.cmd clean spring-boot:run
```

Open `http://localhost:8082/api/health`. Expected:
`AI Job Outreach Agent is running!`

## 4. Configure your candidate profile

Use Postman: `PUT http://localhost:8082/api/profile`
Header: `Content-Type: application/json`

Example body — edit every value so it is accurate:

```json
{
  "fullName": "Your Full Name",
  "headline": "Entry-Level Java Backend Developer",
  "degree": "B.Tech Computer Science and Engineering",
  "graduationYear": "2025",
  "skills": "Java, Spring Boot, REST APIs, MySQL, Hibernate/JPA, Spring Security, JWT",
  "projects": "JobTracker: Spring Boot job application tracker with JWT authentication, MySQL and reminders. URL Shortener: Spring Boot REST API with MySQL and caching.",
  "preferredLocations": "Noida, Delhi NCR, Bengaluru, Hyderabad",
  "githubUrl": "https://github.com/yourusername",
  "linkedinUrl": "https://linkedin.com/in/yourprofile"
}
```

Do not claim experience or skills you do not have.

## 5. Upload resume PDF

`POST http://localhost:8082/api/profile/resume`

In Postman choose Body → form-data:
- Key: `file`
- Change type from Text to File
- Select your PDF

The file is stored under `uploads/candidate-resume.pdf`. Keep the `uploads` folder backed up and private.

## 6. Import HR contacts from Excel

Create an `.xlsx` file. The first worksheet must have these exact header names (case-insensitive):

| name | email | company | jobTitle | linkedinUrl | notes |
|---|---|---|---|---|---|
| Recruiter Name | recruiter@example.com | Example Ltd | Talent Acquisition | https://linkedin.com/in/example | Java fresher hiring |

Required: `name`, `email`. Other columns optional. Existing email addresses are skipped. Import:

`POST http://localhost:8082/api/import/excel`

Postman → Body → form-data:
- Key: `file`
- Type: File
- Select `.xlsx`

## 7. Create / view contacts

- `GET http://localhost:8082/api/contacts`
- `POST http://localhost:8082/api/contacts`
- `GET http://localhost:8082/api/contacts/1`
- `PUT http://localhost:8082/api/contacts/1`
- `DELETE http://localhost:8082/api/contacts/1`

POST example:

```json
{
  "name": "Recruiter Name",
  "email": "recruiter@example.com",
  "company": "Example Ltd",
  "jobTitle": "Talent Acquisition",
  "linkedinUrl": "https://linkedin.com/in/example",
  "notes": "Saw Java Backend Developer opening on company careers page."
}
```

## 8. Generate, review, approve, send

1. Generate a draft for contact ID 1:
   `POST http://localhost:8082/api/drafts/generate/1`
2. View all drafts:
   `GET http://localhost:8082/api/drafts`
3. Edit draft while it is still DRAFT:
   `PUT http://localhost:8082/api/drafts/1`

```json
{
  "subject": "Application inquiry — Java Backend Developer",
  "body": "Hello ... "
}
```

4. Read and review the final subject/body and recipient. Approve only if correct:
   `POST http://localhost:8082/api/drafts/1/approve`
5. Send the approved draft:
   `POST http://localhost:8082/api/drafts/1/send`

The send endpoint rejects anything that is not APPROVED. If a resume PDF was uploaded, it is attached. Sending uses Gmail SMTP and requires Gmail configuration.

Statuses: `DRAFT → APPROVED → SENT` or `DRAFT → REJECTED`.

## API summary

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/health` | Health check |
| PUT / GET | `/api/profile` | Save / view candidate profile |
| POST | `/api/profile/resume` | Upload PDF resume |
| POST | `/api/import/excel` | Import `.xlsx` contacts |
| GET / POST | `/api/contacts` | List / create contacts |
| GET / PUT / DELETE | `/api/contacts/{id}` | Contact operations |
| POST | `/api/drafts/generate/{contactId}` | Generate AI draft |
| GET | `/api/drafts` | List drafts |
| PUT | `/api/drafts/{id}` | Edit a DRAFT |
| POST | `/api/drafts/{id}/approve` | Human approval |
| POST | `/api/drafts/{id}/reject` | Reject draft |
| POST | `/api/drafts/{id}/send` | Send approved draft |

## Gmail App Password setup

1. Enable 2-Step Verification on your Google account.
2. Open your Google Account → Security → search for App passwords.
3. Create an app password and use it as `GMAIL_APP_PASSWORD`.
4. Use your Gmail address as `GMAIL_USERNAME`.
5. Never use your normal Gmail account password in this app.

Google may restrict App Password availability for some managed/workspace accounts. If unavailable, Gmail OAuth/API integration is a separate future enhancement.

## Troubleshooting

- MySQL access denied: verify `DB_USERNAME` and `DB_PASSWORD`; never paste passwords into chat.
- Port 8082 busy: stop the other process or change `server.port`.
- `OPENAI_API_KEY is not configured`: set the environment variable and restart the app.
- Gmail authentication failure: confirm App Password, 2-Step Verification, and Gmail account policy.
- Excel import error: use `.xlsx`, first sheet, headers `name` and `email`.
- IntelliJ red imports: open the extracted root folder as a Maven project, right-click `pom.xml` → Add as Maven Project (if shown), then Maven → Reload All Projects. Run the Maven command from the terminal or IntelliJ Maven panel; the Maven import should finish before editing.

## Safety / sending practice

Start with one contact you know and a test draft. Verify the recipient, subject, body, and attachment before approval. Do not bulk-send unsolicited messages. Respect company contact preferences and applicable anti-spam rules.
