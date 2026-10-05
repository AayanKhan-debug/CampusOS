# ══════════════════════════════════════════════════════════════
#  CampusOS Phase 3 — Events & Registration Integration Tests
# ══════════════════════════════════════════════════════════════

$BASE = "http://localhost:8080"
$pass = 0
$fail = 0
$uniqueSuffix = [System.DateTime]::Now.Ticks

function Test-API {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Url,
        [string]$Body = $null,
        [string]$Token = $null,
        [int]$ExpectedStatus = 200,
        [string]$ExpectContains = $null
    )
    $headers = @{ "Content-Type" = "application/json" }
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    
    try {
        $params = @{
            Uri = $Url
            Method = $Method
            Headers = $headers
            ErrorAction = "Stop"
        }
        if ($Body) { $params["Body"] = $Body }
        
        $response = Invoke-WebRequest @params
        $statusCode = $response.StatusCode
        $content = $response.Content
    } catch {
        $statusCode = [int]$_.Exception.Response.StatusCode
        try { $content = $_.ErrorDetails.Message } catch { $content = "" }
    }
    
    $statusMatch = $statusCode -eq $ExpectedStatus
    $containsMatch = if ($ExpectContains) { $content -match [regex]::Escape($ExpectContains) } else { $true }
    
    if ($statusMatch -and $containsMatch) {
        Write-Host "  PASS: $Name (HTTP $statusCode)" -ForegroundColor Green
        $script:pass++
    } else {
        Write-Host "  FAIL: $Name (Expected: $ExpectedStatus, Got: $statusCode)" -ForegroundColor Red
        if ($ExpectContains -and -not $containsMatch) {
            Write-Host "        Expected to contain: $ExpectContains" -ForegroundColor Red
        }
        $script:fail++
    }
    
    return $content
}

Write-Host "`n===== PHASE 3: EVENTS & REGISTRATION TESTS =====" -ForegroundColor Cyan

# ── 1. Login users ──────────────────────────────────────────
Write-Host "`n--- Step 1: Login all users ---" -ForegroundColor Yellow

# CLUB_ADMIN
$loginResp = Invoke-RestMethod -Uri "$BASE/api/auth/login" -Method POST -ContentType "application/json" -Body '{"email":"leader@campus.edu","password":"password123"}'
$CLUB_ADMIN_TOKEN = $loginResp.token
Write-Host "  Logged in as CLUB_ADMIN (leader@campus.edu)" -ForegroundColor Green

# ADMIN
$loginResp = Invoke-RestMethod -Uri "$BASE/api/auth/login" -Method POST -ContentType "application/json" -Body '{"email":"admin@campus.edu","password":"admin123"}'
$ADMIN_TOKEN = $loginResp.token
Write-Host "  Logged in as ADMIN (admin@campus.edu)" -ForegroundColor Green

# STUDENT 1
$loginResp = Invoke-RestMethod -Uri "$BASE/api/auth/login" -Method POST -ContentType "application/json" -Body '{"email":"student1@campus.edu","password":"password123"}'
$STUDENT1_TOKEN = $loginResp.token
Write-Host "  Logged in as STUDENT (student1@campus.edu)" -ForegroundColor Green

# STUDENT 2
$loginResp = Invoke-RestMethod -Uri "$BASE/api/auth/login" -Method POST -ContentType "application/json" -Body '{"email":"student2@campus.edu","password":"password123"}'
$STUDENT2_TOKEN = $loginResp.token
Write-Host "  Logged in as STUDENT (student2@campus.edu)" -ForegroundColor Green

# ── 2. Create Event Tests ──────────────────────────────────
Write-Host "`n--- Step 2: Create Event ---" -ForegroundColor Yellow

# First, ensure there's an active club
$clubBody = @{
    name = "Phase3 Test Club $uniqueSuffix"
    description = "Club for Phase 3 event testing"
    category = "Technical"
} | ConvertTo-Json
try {
    $clubResp = Invoke-RestMethod -Uri "$BASE/api/clubs" -Method POST -ContentType "application/json" -Headers @{Authorization="Bearer $CLUB_ADMIN_TOKEN"} -Body $clubBody
    $CLUB_ID = $clubResp.id
    Write-Host "  Created club ID: $CLUB_ID" -ForegroundColor Gray
} catch {
    $CLUB_ID = 1
    Write-Host "  Using existing club ID: $CLUB_ID" -ForegroundColor Gray
}

# Test: CLUB_ADMIN creates event for their club
$futureDate = (Get-Date).AddDays(7).ToString("yyyy-MM-dd")
$eventBody = @{
    title = "Spring Boot Workshop $uniqueSuffix"
    description = "Hands-on workshop on Spring Boot 3"
    category = "WORKSHOP"
    date = $futureDate
    startTime = "10:00:00"
    endTime = "12:00:00"
    location = "Auditorium A"
    capacity = 3
    clubId = $CLUB_ID
} | ConvertTo-Json
$eventContent = Test-API -Name "CLUB_ADMIN creates event for own club" -Method POST -Url "$BASE/api/events" -Body $eventBody -Token $CLUB_ADMIN_TOKEN -ExpectedStatus 201 -ExpectContains "Spring Boot Workshop"
$eventObj = $eventContent | ConvertFrom-Json
$EVENT_ID = $eventObj.id
Write-Host "  Event ID: $EVENT_ID" -ForegroundColor Gray

# Test: ADMIN creates event without club
$eventBody2 = @{
    title = "Campus Fest $uniqueSuffix"
    description = "Annual campus festival"
    category = "CULTURAL"
    date = $futureDate
    startTime = "09:00:00"
    endTime = "17:00:00"
    location = "Main Ground"
} | ConvertTo-Json
$eventContent2 = Test-API -Name "ADMIN creates event without club" -Method POST -Url "$BASE/api/events" -Body $eventBody2 -Token $ADMIN_TOKEN -ExpectedStatus 201 -ExpectContains "Campus Fest"
$event2Obj = $eventContent2 | ConvertFrom-Json
$EVENT2_ID = $event2Obj.id

# Test: STUDENT cannot create events
$eventBody3 = @{
    title = "Student Event"
    category = "OTHER"
    date = $futureDate
    startTime = "10:00:00"
    endTime = "12:00:00"
    location = "Room 101"
} | ConvertTo-Json
Test-API -Name "STUDENT cannot create events (403)" -Method POST -Url "$BASE/api/events" -Body $eventBody3 -Token $STUDENT1_TOKEN -ExpectedStatus 403

# Test: Invalid time (endTime before startTime)
$badTimeBody = @{
    title = "Bad Time Event"
    category = "OTHER"
    date = $futureDate
    startTime = "14:00:00"
    endTime = "10:00:00"
    location = "Room 101"
} | ConvertTo-Json
Test-API -Name "End time before start time (400)" -Method POST -Url "$BASE/api/events" -Body $badTimeBody -Token $ADMIN_TOKEN -ExpectedStatus 400 -ExpectContains "End time"

# ── 3. Get Events Tests ────────────────────────────────────
Write-Host "`n--- Step 3: Get Events ---" -ForegroundColor Yellow

Test-API -Name "Get all events (paginated)" -Method GET -Url "$BASE/api/events" -Token $STUDENT1_TOKEN -ExpectedStatus 200 -ExpectContains "content"

Test-API -Name "Get event by ID" -Method GET -Url "$BASE/api/events/$EVENT_ID" -Token $STUDENT1_TOKEN -ExpectedStatus 200 -ExpectContains "Spring Boot Workshop"

Test-API -Name "Filter events by category" -Method GET -Url "$BASE/api/events?category=WORKSHOP" -Token $STUDENT1_TOKEN -ExpectedStatus 200

Test-API -Name "Get nonexistent event (404)" -Method GET -Url "$BASE/api/events/99999" -Token $STUDENT1_TOKEN -ExpectedStatus 404

# ── 4. Update Event Tests ──────────────────────────────────
Write-Host "`n--- Step 4: Update Event ---" -ForegroundColor Yellow

$updateBody = @{
    title = "Updated Workshop Title $uniqueSuffix"
    location = "New Auditorium"
} | ConvertTo-Json
Test-API -Name "Creator updates event" -Method PUT -Url "$BASE/api/events/$EVENT_ID" -Body $updateBody -Token $CLUB_ADMIN_TOKEN -ExpectedStatus 200 -ExpectContains "Updated Workshop Title"

Test-API -Name "STUDENT cannot update event (403)" -Method PUT -Url "$BASE/api/events/$EVENT_ID" -Body $updateBody -Token $STUDENT1_TOKEN -ExpectedStatus 403

# ── 5. Registration Tests ──────────────────────────────────
Write-Host "`n--- Step 5: Event Registration ---" -ForegroundColor Yellow

# Register student1 → should be CONFIRMED (capacity=3, 0 registered)
$reg1Content = Test-API -Name "Student1 registers for event (CONFIRMED)" -Method POST -Url "$BASE/api/events/$EVENT_ID/register" -Token $STUDENT1_TOKEN -ExpectedStatus 201 -ExpectContains "CONFIRMED"

# Register student2 → should be CONFIRMED (capacity=3, 1 registered)
$reg2Content = Test-API -Name "Student2 registers for event (CONFIRMED)" -Method POST -Url "$BASE/api/events/$EVENT_ID/register" -Token $STUDENT2_TOKEN -ExpectedStatus 201 -ExpectContains "CONFIRMED"

# Register admin → should be CONFIRMED (capacity=3, 2 registered)
$reg3Content = Test-API -Name "Admin registers for event (CONFIRMED, 3rd slot)" -Method POST -Url "$BASE/api/events/$EVENT_ID/register" -Token $ADMIN_TOKEN -ExpectedStatus 201 -ExpectContains "CONFIRMED"

# Register CLUB_ADMIN → should be WAITLISTED (capacity=3, 3 confirmed = full)
$reg4Content = Test-API -Name "CLUB_ADMIN registers (WAITLISTED, capacity full)" -Method POST -Url "$BASE/api/events/$EVENT_ID/register" -Token $CLUB_ADMIN_TOKEN -ExpectedStatus 201 -ExpectContains "WAITLISTED"

# Test: duplicate registration → 409
Test-API -Name "Duplicate registration blocked (409)" -Method POST -Url "$BASE/api/events/$EVENT_ID/register" -Token $STUDENT1_TOKEN -ExpectedStatus 409 -ExpectContains "already have an active"

# ── 6. View Registrations (Admin) ──────────────────────────
Write-Host "`n--- Step 6: View Registrations ---" -ForegroundColor Yellow

Test-API -Name "Event creator views registrations" -Method GET -Url "$BASE/api/events/$EVENT_ID/registrations" -Token $CLUB_ADMIN_TOKEN -ExpectedStatus 200 -ExpectContains "content"

Test-API -Name "STUDENT cannot view registrations (403)" -Method GET -Url "$BASE/api/events/$EVENT_ID/registrations" -Token $STUDENT1_TOKEN -ExpectedStatus 403

# ── 7. My Registered Events ───────────────────────────────
Write-Host "`n--- Step 7: My Registered Events ---" -ForegroundColor Yellow

Test-API -Name "Student1 views their registered events" -Method GET -Url "$BASE/api/users/me/events" -Token $STUDENT1_TOKEN -ExpectedStatus 200 -ExpectContains "content"

Test-API -Name "Student1 filters by CONFIRMED status" -Method GET -Url "$BASE/api/users/me/events?status=CONFIRMED" -Token $STUDENT1_TOKEN -ExpectedStatus 200

# ── 8. Cancel Registration & Waitlist Promotion ───────────
Write-Host "`n--- Step 8: Cancel Registration & Waitlist Promotion ---" -ForegroundColor Yellow

# Student1 cancels (CONFIRMED) → CLUB_ADMIN (WAITLISTED) should be promoted to CONFIRMED
Test-API -Name "Student1 cancels registration" -Method DELETE -Url "$BASE/api/events/$EVENT_ID/register" -Token $STUDENT1_TOKEN -ExpectedStatus 200 -ExpectContains "CANCELLED"

# Verify CLUB_ADMIN got promoted — check their registration by viewing their events
$myEventsContent = Test-API -Name "CLUB_ADMIN views events (should be CONFIRMED after promotion)" -Method GET -Url "$BASE/api/users/me/events?status=CONFIRMED" -Token $CLUB_ADMIN_TOKEN -ExpectedStatus 200

# Double cancel → error
Test-API -Name "Student1 double-cancel (400)" -Method DELETE -Url "$BASE/api/events/$EVENT_ID/register" -Token $STUDENT1_TOKEN -ExpectedStatus 400 -ExpectContains "already cancelled"

# ── 9. Cancel Event ───────────────────────────────────────
Write-Host "`n--- Step 9: Cancel Event ---" -ForegroundColor Yellow

# Create a new event to cancel
$cancelEventBody = @{
    title = "Event To Cancel $uniqueSuffix"
    category = "OTHER"
    date = $futureDate
    startTime = "15:00:00"
    endTime = "16:00:00"
    location = "Room 303"
    capacity = 10
} | ConvertTo-Json
$cancelEventContent = Test-API -Name "Create event to test cancellation" -Method POST -Url "$BASE/api/events" -Body $cancelEventBody -Token $ADMIN_TOKEN -ExpectedStatus 201
$cancelEventObj = $cancelEventContent | ConvertFrom-Json
$CANCEL_EVENT_ID = $cancelEventObj.id

# Register a student for it
Test-API -Name "Student registers for event-to-cancel" -Method POST -Url "$BASE/api/events/$CANCEL_EVENT_ID/register" -Token $STUDENT1_TOKEN -ExpectedStatus 201

# Cancel the event
Test-API -Name "Admin cancels event" -Method DELETE -Url "$BASE/api/events/$CANCEL_EVENT_ID" -Token $ADMIN_TOKEN -ExpectedStatus 204

# Verify event is cancelled
Test-API -Name "Cancelled event shows CANCELLED status" -Method GET -Url "$BASE/api/events/$CANCEL_EVENT_ID" -Token $STUDENT1_TOKEN -ExpectedStatus 200 -ExpectContains "CANCELLED"

# Cannot register for cancelled event
Test-API -Name "Cannot register for cancelled event (400)" -Method POST -Url "$BASE/api/events/$CANCEL_EVENT_ID/register" -Token $STUDENT2_TOKEN -ExpectedStatus 400 -ExpectContains "cancelled"

# ── 10. Phase 1 & 2 Regression ───────────────────────────
Write-Host "`n--- Step 10: Phase 1 & 2 Regression Checks ---" -ForegroundColor Yellow

Test-API -Name "Phase 1: Health endpoint" -Method GET -Url "$BASE/actuator/health" -ExpectedStatus 200 -ExpectContains "UP"

Test-API -Name "Phase 1: GET /api/users/me" -Method GET -Url "$BASE/api/users/me" -Token $STUDENT1_TOKEN -ExpectedStatus 200 -ExpectContains "student1@campus.edu"

Test-API -Name "Phase 2: GET /api/clubs" -Method GET -Url "$BASE/api/clubs" -Token $STUDENT1_TOKEN -ExpectedStatus 200 -ExpectContains "content"

# ── Summary ───────────────────────────────────────────────
Write-Host "`n===== TEST RESULTS =====" -ForegroundColor Cyan
Write-Host "  PASSED: $pass" -ForegroundColor Green
Write-Host "  FAILED: $fail" -ForegroundColor $(if ($fail -gt 0) { "Red" } else { "Green" })
Write-Host "  TOTAL:  $($pass + $fail)" -ForegroundColor Cyan
