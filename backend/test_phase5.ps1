# ══════════════════════════════════════════════════════════════
#  CampusOS Phase 5 — Lost & Found, Opportunities & Help Desk Tests
# ══════════════════════════════════════════════════════════════

$BASE = "http://localhost:8080"
$pass = 0; $fail = 0
$suffix = Get-Random -Minimum 1000000 -Maximum 9999999

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
        $params = @{ Uri=$Url; Method=$Method; Headers=$headers; ErrorAction="Stop" }
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
            Write-Host "        Expected: '$ExpectContains'" -ForegroundColor Red
            Write-Host "        Content: $($content.Substring(0, [Math]::Min(200,$content.Length)))" -ForegroundColor DarkRed
        }
        $script:fail++
    }
    return $content
}

Write-Host "`n===== PHASE 5: LOST & FOUND + OPPORTUNITIES + CAMPUS HELP DESK TESTS =====" -ForegroundColor Cyan

# ── 1. Login ──────────────────────────────────────────────────
Write-Host "`n--- Step 1: Login ---" -ForegroundColor Yellow
$ADMIN_TOKEN = (Invoke-RestMethod -Uri "$BASE/api/auth/login" -Method POST -ContentType "application/json" -Body '{"email":"admin@campus.edu","password":"admin123"}').token
$LEADER_TOKEN = (Invoke-RestMethod -Uri "$BASE/api/auth/login" -Method POST -ContentType "application/json" -Body '{"email":"leader@campus.edu","password":"password123"}').token
$STUDENT1_TOKEN = (Invoke-RestMethod -Uri "$BASE/api/auth/login" -Method POST -ContentType "application/json" -Body '{"email":"student1@campus.edu","password":"password123"}').token
$STUDENT2_TOKEN = (Invoke-RestMethod -Uri "$BASE/api/auth/login" -Method POST -ContentType "application/json" -Body '{"email":"student2@campus.edu","password":"password123"}').token
Write-Host "  Tokens acquired for admin, leader, student1, student2" -ForegroundColor Gray

# ═══════════════════════════════════════════
#  MODULE 1: LOST & FOUND
# ═══════════════════════════════════════════
Write-Host "`n══ MODULE 1: LOST & FOUND ══" -ForegroundColor Magenta

# ── Create ────────────────────────────────
Write-Host "`n--- Lost & Found: Create ---" -ForegroundColor Yellow
$lfBody1 = @{
    title = "Lost Water Bottle $suffix"
    description = "Hydroflask steel bottle left in Room 402"
    itemType = "Bottle"
    listingType = "LOST"
    location = "Room 402"
    dateOccurred = (Get-Date).ToString("yyyy-MM-dd")
    contactInfo = "student1@campus.edu"
} | ConvertTo-Json
$lfResp1 = Test-API -Name "Student1 reports LOST item" -Method POST -Url "$BASE/api/lost-found" -Body $lfBody1 -Token $STUDENT1_TOKEN -ExpectedStatus 201 -ExpectContains "LOST"
$lf1 = $lfResp1 | ConvertFrom-Json
$LF1_ID = $lf1.id

$lfBody2 = @{
    title = "Found Umbrella $suffix"
    description = "Black umbrella found near entrance"
    itemType = "Umbrella"
    listingType = "FOUND"
    location = "Main Gate"
    dateOccurred = (Get-Date).ToString("yyyy-MM-dd")
} | ConvertTo-Json
$lfResp2 = Test-API -Name "Student2 reports FOUND item" -Method POST -Url "$BASE/api/lost-found" -Body $lfBody2 -Token $STUDENT2_TOKEN -ExpectedStatus 201 -ExpectContains "FOUND"
$lf2 = $lfResp2 | ConvertFrom-Json
$LF2_ID = $lf2.id

# Future dateOccurred rejection
$badDateLf = @{
    title = "Future Item"
    itemType = "Book"
    listingType = "LOST"
    location = "Library"
    dateOccurred = (Get-Date).AddDays(5).ToString("yyyy-MM-dd")
} | ConvertTo-Json
Test-API -Name "Date in future → 400" -Method POST -Url "$BASE/api/lost-found" -Body $badDateLf -Token $STUDENT1_TOKEN -ExpectedStatus 400 -ExpectContains "future"

# ── Read & Filter ─────────────────────────
Write-Host "`n--- Lost & Found: Read & Filter ---" -ForegroundColor Yellow
Test-API -Name "GET all items (default feed: OPEN, MATCHED)" -Method GET -Url "$BASE/api/lost-found" -Token $STUDENT1_TOKEN -ExpectedStatus 200 -ExpectContains "content"
Test-API -Name "GET item by ID" -Method GET -Url "$BASE/api/lost-found/$LF1_ID" -Token $STUDENT1_TOKEN -ExpectedStatus 200 -ExpectContains "Lost Water Bottle"
Test-API -Name "Filter by listingType=LOST" -Method GET -Url "$BASE/api/lost-found?listingType=LOST" -Token $STUDENT1_TOKEN -ExpectedStatus 200 -ExpectContains "LOST"
Test-API -Name "Filter by status=OPEN" -Method GET -Url "$BASE/api/lost-found?status=OPEN" -Token $STUDENT1_TOKEN -ExpectedStatus 200 -ExpectContains "OPEN"
Test-API -Name "Search by keyword 'Water'" -Method GET -Url "$BASE/api/lost-found?search=Water" -Token $STUDENT1_TOKEN -ExpectedStatus 200 -ExpectContains "Lost Water Bottle"

# ── Update & Ownership ─────────────────────
Write-Host "`n--- Lost & Found: Update & Permissions ---" -ForegroundColor Yellow
$updateLf = @{ title = "Lost Water Bottle (Blue) $suffix" } | ConvertTo-Json
Test-API -Name "Owner updates listing" -Method PUT -Url "$BASE/api/lost-found/$LF1_ID" -Body $updateLf -Token $STUDENT1_TOKEN -ExpectedStatus 200 -ExpectContains "Blue"
Test-API -Name "Student2 cannot update Student1's listing (403)" -Method PUT -Url "$BASE/api/lost-found/$LF1_ID" -Body $updateLf -Token $STUDENT2_TOKEN -ExpectedStatus 403

# ── Status Transitions ────────────────────
Write-Host "`n--- Lost & Found: Status Transitions ---" -ForegroundColor Yellow
$toMatched = @{ status = "MATCHED" } | ConvertTo-Json
Test-API -Name "Valid transition: OPEN → MATCHED" -Method PATCH -Url "$BASE/api/lost-found/$LF1_ID/status" -Body $toMatched -Token $STUDENT1_TOKEN -ExpectedStatus 200 -ExpectContains "MATCHED"

$toRecovered = @{ status = "RECOVERED" } | ConvertTo-Json
Test-API -Name "Valid transition: MATCHED → RECOVERED" -Method PATCH -Url "$BASE/api/lost-found/$LF1_ID/status" -Body $toRecovered -Token $STUDENT1_TOKEN -ExpectedStatus 200 -ExpectContains "RECOVERED"

$toOpen = @{ status = "OPEN" } | ConvertTo-Json
Test-API -Name "Invalid transition: RECOVERED → OPEN (400)" -Method PATCH -Url "$BASE/api/lost-found/$LF1_ID/status" -Body $toOpen -Token $STUDENT1_TOKEN -ExpectedStatus 400 -ExpectContains "Invalid status transition"

$toClosed = @{ status = "CLOSED" } | ConvertTo-Json
Test-API -Name "Valid transition: RECOVERED → CLOSED" -Method PATCH -Url "$BASE/api/lost-found/$LF1_ID/status" -Body $toClosed -Token $STUDENT1_TOKEN -ExpectedStatus 200 -ExpectContains "CLOSED"

# Delete
Test-API -Name "Student2 cannot delete Student1's listing (403)" -Method DELETE -Url "$BASE/api/lost-found/$LF1_ID" -Token $STUDENT2_TOKEN -ExpectedStatus 403
Test-API -Name "Admin deletes listing (cleanup)" -Method DELETE -Url "$BASE/api/lost-found/$LF1_ID" -Token $ADMIN_TOKEN -ExpectedStatus 204
Test-API -Name "Owner deletes own listing (cleanup)" -Method DELETE -Url "$BASE/api/lost-found/$LF2_ID" -Token $STUDENT2_TOKEN -ExpectedStatus 204

# ═══════════════════════════════════════════
#  MODULE 2: OPPORTUNITIES
# ═══════════════════════════════════════════
Write-Host "`n══ MODULE 2: OPPORTUNITIES ══" -ForegroundColor Magenta

# ── Create ────────────────────────────────
Write-Host "`n--- Opportunities: Create & Authorization ---" -ForegroundColor Yellow
$oppBody1 = @{
    title = "Backend Intern $suffix"
    description = "Exciting backend engineering internship"
    organization = "Campus Tech"
    opportunityType = "INTERNSHIP"
    eligibility = "2nd and 3rd year CSE/ISE students"
    applicationUrl = "https://campustech.io/careers"
    deadline = (Get-Date).AddDays(20).ToString("yyyy-MM-dd'T'23:59:59")
    location = "Bengaluru"
    remote = $true
} | ConvertTo-Json

# Student cannot create
Test-API -Name "Student cannot create opportunity (403)" -Method POST -Url "$BASE/api/opportunities" -Body $oppBody1 -Token $STUDENT1_TOKEN -ExpectedStatus 403

# CLUB_ADMIN creates
$oppResp1 = Test-API -Name "CLUB_ADMIN creates opportunity" -Method POST -Url "$BASE/api/opportunities" -Body $oppBody1 -Token $LEADER_TOKEN -ExpectedStatus 201 -ExpectContains "INTERNSHIP"
$opp1 = $oppResp1 | ConvertFrom-Json
$OPP1_ID = $opp1.id

# ADMIN creates
$oppBody2 = @{
    title = "Hackathon India $suffix"
    description = "National level hackathon with cash prizes"
    organization = "National Coding Foundation"
    opportunityType = "HACKATHON"
    applicationUrl = "https://hackindia.org"
    deadline = (Get-Date).AddDays(15).ToString("yyyy-MM-dd'T'18:00:00")
    remote = $false
} | ConvertTo-Json
$oppResp2 = Test-API -Name "ADMIN creates opportunity" -Method POST -Url "$BASE/api/opportunities" -Body $oppBody2 -Token $ADMIN_TOKEN -ExpectedStatus 201 -ExpectContains "HACKATHON"
$opp2 = $oppResp2 | ConvertFrom-Json
$OPP2_ID = $opp2.id

# Validation checks
$badDeadlineOpp = @{
    title = "Past Deadline Opp"
    description = "Description"
    organization = "Old Org"
    opportunityType = "COMPETITION"
    deadline = (Get-Date).AddDays(-2).ToString("yyyy-MM-dd'T'12:00:00")
} | ConvertTo-Json
Test-API -Name "Past deadline → 400" -Method POST -Url "$BASE/api/opportunities" -Body $badDeadlineOpp -Token $ADMIN_TOKEN -ExpectedStatus 400 -ExpectContains "past"

$badUrlOpp = @{
    title = "Bad URL Opp"
    description = "Description"
    organization = "Org"
    opportunityType = "WORKSHOP"
    applicationUrl = "ftp://bad-url.com"
} | ConvertTo-Json
Test-API -Name "Invalid URL protocol → 400" -Method POST -Url "$BASE/api/opportunities" -Body $badUrlOpp -Token $ADMIN_TOKEN -ExpectedStatus 400 -ExpectContains "http:// or https://"

# ── Read & Filter ─────────────────────────
Write-Host "`n--- Opportunities: Read & Filter ---" -ForegroundColor Yellow
Test-API -Name "GET opportunities (default: active, nearest deadline)" -Method GET -Url "$BASE/api/opportunities" -Token $STUDENT1_TOKEN -ExpectedStatus 200 -ExpectContains "content"
Test-API -Name "GET opportunity by ID" -Method GET -Url "$BASE/api/opportunities/$OPP1_ID" -Token $STUDENT1_TOKEN -ExpectedStatus 200 -ExpectContains "Backend Intern"
Test-API -Name "Filter by type=INTERNSHIP" -Method GET -Url "$BASE/api/opportunities?type=INTERNSHIP" -Token $STUDENT1_TOKEN -ExpectedStatus 200 -ExpectContains "INTERNSHIP"
Test-API -Name "Filter by remote=true" -Method GET -Url "$BASE/api/opportunities?remote=true" -Token $STUDENT1_TOKEN -ExpectedStatus 200 -ExpectContains "true"
Test-API -Name "Search keyword 'Hackathon'" -Method GET -Url "$BASE/api/opportunities?search=Hackathon" -Token $STUDENT1_TOKEN -ExpectedStatus 200 -ExpectContains "Hackathon"

# ── Update & Delete Permissions ───────────
Write-Host "`n--- Opportunities: Update & Delete Permissions ---" -ForegroundColor Yellow
$updateOpp = @{ title = "Backend Intern (Updated) $suffix" } | ConvertTo-Json
Test-API -Name "Creator updates opportunity" -Method PUT -Url "$BASE/api/opportunities/$OPP1_ID" -Body $updateOpp -Token $LEADER_TOKEN -ExpectedStatus 200 -ExpectContains "Updated"
Test-API -Name "Student cannot update opportunity (403)" -Method PUT -Url "$BASE/api/opportunities/$OPP1_ID" -Body $updateOpp -Token $STUDENT1_TOKEN -ExpectedStatus 403

Test-API -Name "Student cannot delete opportunity (403)" -Method DELETE -Url "$BASE/api/opportunities/$OPP1_ID" -Token $STUDENT1_TOKEN -ExpectedStatus 403
Test-API -Name "Creator deletes own opportunity" -Method DELETE -Url "$BASE/api/opportunities/$OPP1_ID" -Token $LEADER_TOKEN -ExpectedStatus 204
Test-API -Name "Admin deletes opportunity (cleanup)" -Method DELETE -Url "$BASE/api/opportunities/$OPP2_ID" -Token $ADMIN_TOKEN -ExpectedStatus 204

# ═══════════════════════════════════════════
#  MODULE 3: CAMPUS HELP DESK
# ═══════════════════════════════════════════
Write-Host "`n══ MODULE 3: CAMPUS HELP DESK ══" -ForegroundColor Magenta

# ── Create ────────────────────────────────
Write-Host "`n--- Help Desk: Create ---" -ForegroundColor Yellow
$issueBody1 = @{
    title = "Wi-Fi not working $suffix"
    description = "No internet connectivity in Classroom 201"
    category = "INTERNET"
    location = "Room 201"
    priority = "HIGH"
} | ConvertTo-Json
$issueResp1 = Test-API -Name "Student1 submits issue (SUBMITTED)" -Method POST -Url "$BASE/api/help-desk" -Body $issueBody1 -Token $STUDENT1_TOKEN -ExpectedStatus 201 -ExpectContains "SUBMITTED"
$issue1 = $issueResp1 | ConvertFrom-Json
$ISSUE1_ID = $issue1.id

$issueBody2 = @{
    title = "Water leakage in Hostel B $suffix"
    description = "Pipe broken on 2nd floor corridor"
    category = "WATER"
    location = "Hostel B 2nd floor"
    priority = "URGENT"
} | ConvertTo-Json
$issueResp2 = Test-API -Name "Student2 submits URGENT issue" -Method POST -Url "$BASE/api/help-desk" -Body $issueBody2 -Token $STUDENT2_TOKEN -ExpectedStatus 201 -ExpectContains "URGENT"
$issue2 = $issueResp2 | ConvertFrom-Json
$ISSUE2_ID = $issue2.id

# ── Read & Priority Ordering ──────────────
Write-Host "`n--- Help Desk: Read & Priority Ordering ---" -ForegroundColor Yellow
Test-API -Name "Student1 views my issues (/api/users/me/issues)" -Method GET -Url "$BASE/api/users/me/issues" -Token $STUDENT1_TOKEN -ExpectedStatus 200 -ExpectContains "Wi-Fi not working"
Test-API -Name "Student1 views own issue by ID" -Method GET -Url "$BASE/api/help-desk/$ISSUE1_ID" -Token $STUDENT1_TOKEN -ExpectedStatus 200 -ExpectContains "Wi-Fi not working"
Test-API -Name "Student2 cannot view Student1's issue (403)" -Method GET -Url "$BASE/api/help-desk/$ISSUE1_ID" -Token $STUDENT2_TOKEN -ExpectedStatus 403

Test-API -Name "Student cannot list all issues (403)" -Method GET -Url "$BASE/api/help-desk" -Token $STUDENT1_TOKEN -ExpectedStatus 403
Test-API -Name "Admin lists all issues (priority sorted)" -Method GET -Url "$BASE/api/help-desk" -Token $ADMIN_TOKEN -ExpectedStatus 200 -ExpectContains "content"

# ── Update while SUBMITTED ────────────────
Write-Host "`n--- Help Desk: Update & Assign ---" -ForegroundColor Yellow
$updateIssue = @{ title = "Wi-Fi down in Room 201 (Updated) $suffix" } | ConvertTo-Json
Test-API -Name "Reporter updates issue while SUBMITTED" -Method PUT -Url "$BASE/api/help-desk/$ISSUE1_ID" -Body $updateIssue -Token $STUDENT1_TOKEN -ExpectedStatus 200 -ExpectContains "Updated"

# ── Admin Assignment & In-Progress ────────
$assignBody = @{ assignedToUserId = 5 } | ConvertTo-Json # Admin ID is 5 in seed data
Test-API -Name "Student cannot assign issue (403)" -Method PATCH -Url "$BASE/api/help-desk/$ISSUE1_ID/assign" -Body $assignBody -Token $STUDENT1_TOKEN -ExpectedStatus 403
Test-API -Name "Admin assigns issue → IN_PROGRESS" -Method PATCH -Url "$BASE/api/help-desk/$ISSUE1_ID/assign" -Body $assignBody -Token $ADMIN_TOKEN -ExpectedStatus 200 -ExpectContains "IN_PROGRESS"

# Reporter CANNOT update or delete once IN_PROGRESS
Test-API -Name "Reporter cannot update once IN_PROGRESS (400)" -Method PUT -Url "$BASE/api/help-desk/$ISSUE1_ID" -Body $updateIssue -Token $STUDENT1_TOKEN -ExpectedStatus 400 -ExpectContains "Cannot update issue"
Test-API -Name "Reporter cannot delete once IN_PROGRESS (400)" -Method DELETE -Url "$BASE/api/help-desk/$ISSUE1_ID" -Token $STUDENT1_TOKEN -ExpectedStatus 400 -ExpectContains "Cannot delete issue"

# ── Admin Resolve ─────────────────────────
Write-Host "`n--- Help Desk: Resolve & Close Workflow ---" -ForegroundColor Yellow
$resolveBody = @{ resolutionNotes = "Replaced faulty router and verified connectivity." } | ConvertTo-Json
Test-API -Name "Student cannot resolve issue (403)" -Method PATCH -Url "$BASE/api/help-desk/$ISSUE1_ID/resolve" -Body $resolveBody -Token $STUDENT1_TOKEN -ExpectedStatus 403
Test-API -Name "Admin resolves issue → RESOLVED" -Method PATCH -Url "$BASE/api/help-desk/$ISSUE1_ID/resolve" -Body $resolveBody -Token $ADMIN_TOKEN -ExpectedStatus 200 -ExpectContains "RESOLVED"

# Student closes their own RESOLVED issue
$closeBody = @{ status = "CLOSED" } | ConvertTo-Json
Test-API -Name "Student closes own RESOLVED issue" -Method PATCH -Url "$BASE/api/help-desk/$ISSUE1_ID/status" -Body $closeBody -Token $STUDENT1_TOKEN -ExpectedStatus 200 -ExpectContains "CLOSED"

# Student CANNOT close a SUBMITTED issue directly
Test-API -Name "Student cannot close SUBMITTED issue directly (400)" -Method PATCH -Url "$BASE/api/help-desk/$ISSUE2_ID/status" -Body $closeBody -Token $STUDENT2_TOKEN -ExpectedStatus 400 -ExpectContains "only close an issue that has been resolved"

# Admin deletes issue 2 cleanup
Test-API -Name "Admin deletes issue (cleanup)" -Method DELETE -Url "$BASE/api/help-desk/$ISSUE1_ID" -Token $ADMIN_TOKEN -ExpectedStatus 204
Test-API -Name "Admin deletes issue 2 (cleanup)" -Method DELETE -Url "$BASE/api/help-desk/$ISSUE2_ID" -Token $ADMIN_TOKEN -ExpectedStatus 204

# ═══════════════════════════════════════════
#  Phase 1-4 Regression
# ═══════════════════════════════════════════
Write-Host "`n--- Phase 1-4 Regression ---" -ForegroundColor Yellow
Test-API -Name "P1: Health endpoint" -Method GET -Url "$BASE/actuator/health" -ExpectedStatus 200
Test-API -Name "P1: GET /api/users/me" -Method GET -Url "$BASE/api/users/me" -Token $STUDENT1_TOKEN -ExpectedStatus 200 -ExpectContains "student1@campus.edu"
Test-API -Name "P2: GET /api/clubs" -Method GET -Url "$BASE/api/clubs" -Token $STUDENT1_TOKEN -ExpectedStatus 200 -ExpectContains "content"
Test-API -Name "P3: GET /api/events" -Method GET -Url "$BASE/api/events" -Token $STUDENT1_TOKEN -ExpectedStatus 200 -ExpectContains "content"
Test-API -Name "P4: GET /api/announcements" -Method GET -Url "$BASE/api/announcements" -Token $STUDENT1_TOKEN -ExpectedStatus 200 -ExpectContains "content"
Test-API -Name "P4: GET /api/departments" -Method GET -Url "$BASE/api/departments" -Token $STUDENT1_TOKEN -ExpectedStatus 200 -ExpectContains "content"

# ── Final Summary ─────────────────────────
Write-Host "`n===== PHASE 5 TEST RESULTS =====" -ForegroundColor Cyan
Write-Host "  PASSED: $pass" -ForegroundColor Green
Write-Host "  FAILED: $fail" -ForegroundColor $(if ($fail -gt 0) { "Red" } else { "Green" })
Write-Host "  TOTAL:  $($pass + $fail)" -ForegroundColor Cyan
