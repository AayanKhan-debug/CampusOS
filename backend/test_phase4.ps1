# ══════════════════════════════════════════════════════════════
#  CampusOS Phase 4 — Announcements & Resource Hub Integration Tests
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

Write-Host "`n===== PHASE 4: ANNOUNCEMENTS & RESOURCE HUB TESTS =====" -ForegroundColor Cyan

# ── 1. Login ──────────────────────────────────────────────────
Write-Host "`n--- Step 1: Login ---" -ForegroundColor Yellow
$ADMIN_TOKEN = (Invoke-RestMethod -Uri "$BASE/api/auth/login" -Method POST -ContentType "application/json" -Body '{"email":"admin@campus.edu","password":"admin123"}').token
$LEADER_TOKEN = (Invoke-RestMethod -Uri "$BASE/api/auth/login" -Method POST -ContentType "application/json" -Body '{"email":"leader@campus.edu","password":"password123"}').token
$STUDENT_TOKEN = (Invoke-RestMethod -Uri "$BASE/api/auth/login" -Method POST -ContentType "application/json" -Body '{"email":"student1@campus.edu","password":"password123"}').token
Write-Host "  Logged in: admin, leader, student1" -ForegroundColor Gray

# ═══════════════════════════════════════════
#  MODULE 1: ANNOUNCEMENTS
# ═══════════════════════════════════════════
Write-Host "`n══ MODULE 1: ANNOUNCEMENTS ══" -ForegroundColor Magenta

# ── Create ────────────────────────────────
Write-Host "`n--- Announcements: Create ---" -ForegroundColor Yellow

$ann1Body = @{
    title = "Exam Schedule $suffix"
    content = "The semester exam schedule has been released. Please check the official portal."
    category = "EXAM"
    isPinned = $false
} | ConvertTo-Json
$ann1Resp = Test-API -Name "ADMIN creates EXAM announcement" -Method POST -Url "$BASE/api/announcements" -Body $ann1Body -Token $ADMIN_TOKEN -ExpectedStatus 201 -ExpectContains "EXAM"
$ann1 = $ann1Resp | ConvertFrom-Json
$ANN1_ID = $ann1.id
Write-Host "  Announcement ID: $ANN1_ID" -ForegroundColor Gray

$pinnedBody = @{
    title = "PINNED: Campus Wifi Upgrade $suffix"
    content = "Campus network will be upgraded this weekend. Expect brief outages on Saturday morning."
    category = "GENERAL"
    pinned = $true
} | ConvertTo-Json
$pinnedResp = Test-API -Name "ADMIN creates PINNED general announcement" -Method POST -Url "$BASE/api/announcements" -Body $pinnedBody -Token $ADMIN_TOKEN -ExpectedStatus 201 -ExpectContains "true"
$pinned = $pinnedResp | ConvertFrom-Json
$PINNED_ID = $pinned.id

$clubAnnBody = @{
    title = "Club Meeting $suffix"
    content = "The Robotics Club monthly meeting is scheduled for this Friday at 4 PM in Room 101."
    category = "CLUB"
} | ConvertTo-Json
Test-API -Name "CLUB_ADMIN creates CLUB announcement" -Method POST -Url "$BASE/api/announcements" -Body $clubAnnBody -Token $LEADER_TOKEN -ExpectedStatus 201 -ExpectContains "CLUB"

Test-API -Name "STUDENT cannot create announcements (403)" -Method POST -Url "$BASE/api/announcements" -Body $ann1Body -Token $STUDENT_TOKEN -ExpectedStatus 403

# Invalid dates
$badDateBody = @{
    title = "Bad Dates Test"
    content = "Testing invalid expiry date scenario here."
    category = "GENERAL"
    publishedAt = (Get-Date).AddDays(2).ToString("yyyy-MM-dd'T'HH:mm:ss")
    expiresAt = (Get-Date).AddDays(1).ToString("yyyy-MM-dd'T'HH:mm:ss")
} | ConvertTo-Json
Test-API -Name "expiresAt before publishedAt → 400" -Method POST -Url "$BASE/api/announcements" -Body $badDateBody -Token $ADMIN_TOKEN -ExpectedStatus 400 -ExpectContains "Expiry"

# ── Read/Filter ───────────────────────────
Write-Host "`n--- Announcements: Read & Filter ---" -ForegroundColor Yellow
Test-API -Name "GET all announcements (ACTIVE default)" -Method GET -Url "$BASE/api/announcements" -Token $STUDENT_TOKEN -ExpectedStatus 200 -ExpectContains "content"
Test-API -Name "GET announcement by ID" -Method GET -Url "$BASE/api/announcements/$ANN1_ID" -Token $STUDENT_TOKEN -ExpectedStatus 200 -ExpectContains "EXAM"
Test-API -Name "Filter by category=EXAM" -Method GET -Url "$BASE/api/announcements?category=EXAM" -Token $STUDENT_TOKEN -ExpectedStatus 200 -ExpectContains "content"
Test-API -Name "Filter by category=CLUB" -Method GET -Url "$BASE/api/announcements?category=CLUB" -Token $STUDENT_TOKEN -ExpectedStatus 200
Test-API -Name "Search by title keyword" -Method GET -Url "$BASE/api/announcements?search=Exam" -Token $STUDENT_TOKEN -ExpectedStatus 200
Test-API -Name "GET nonexistent announcement → 404" -Method GET -Url "$BASE/api/announcements/99999" -Token $STUDENT_TOKEN -ExpectedStatus 404

# ── Update ───────────────────────────────
Write-Host "`n--- Announcements: Update ---" -ForegroundColor Yellow
$updateAnnBody = @{ title = "Updated Exam Schedule $suffix" } | ConvertTo-Json
Test-API -Name "Owner updates announcement" -Method PUT -Url "$BASE/api/announcements/$ANN1_ID" -Body $updateAnnBody -Token $ADMIN_TOKEN -ExpectedStatus 200 -ExpectContains "Updated Exam Schedule"
Test-API -Name "STUDENT cannot update (403)" -Method PUT -Url "$BASE/api/announcements/$ANN1_ID" -Body $updateAnnBody -Token $STUDENT_TOKEN -ExpectedStatus 403

# ── Pin/Archive ──────────────────────────
Write-Host "`n--- Announcements: Pin & Archive ---" -ForegroundColor Yellow
Test-API -Name "Toggle pin (unpinned → pinned)" -Method PATCH -Url "$BASE/api/announcements/$ANN1_ID/pin" -Token $ADMIN_TOKEN -ExpectedStatus 200
Test-API -Name "Toggle pin (pinned → unpinned)" -Method PATCH -Url "$BASE/api/announcements/$ANN1_ID/pin" -Token $ADMIN_TOKEN -ExpectedStatus 200
Test-API -Name "Archive announcement" -Method PATCH -Url "$BASE/api/announcements/$ANN1_ID/archive" -Token $ADMIN_TOKEN -ExpectedStatus 200 -ExpectContains "ARCHIVED"
Test-API -Name "Archive already-archived → 400" -Method PATCH -Url "$BASE/api/announcements/$ANN1_ID/archive" -Token $ADMIN_TOKEN -ExpectedStatus 400 -ExpectContains "already archived"

# ── Read Tracking ─────────────────────────
Write-Host "`n--- Announcements: Read Tracking ---" -ForegroundColor Yellow

# Get unread count before
$unreadBefore = Test-API -Name "GET unread announcements (student)" -Method GET -Url "$BASE/api/users/me/announcements/unread" -Token $STUDENT_TOKEN -ExpectedStatus 200

# Mark pinned announcement as read
Test-API -Name "Mark announcement as read" -Method POST -Url "$BASE/api/announcements/$PINNED_ID/read" -Token $STUDENT_TOKEN -ExpectedStatus 201 -ExpectContains "PINNED"

# Duplicate read
Test-API -Name "Duplicate read → 409" -Method POST -Url "$BASE/api/announcements/$PINNED_ID/read" -Token $STUDENT_TOKEN -ExpectedStatus 409 -ExpectContains "already marked"

# ═══════════════════════════════════════════
#  MODULE 2: RESOURCE HUB
# ═══════════════════════════════════════════
Write-Host "`n══ MODULE 2: RESOURCE HUB ══" -ForegroundColor Magenta

# ── Departments ──────────────────────────
Write-Host "`n--- Resource Hub: Departments ---" -ForegroundColor Yellow
$deptBody = @{
    name = "Information Science $suffix"
    code = "ISE$($suffix.ToString().Substring($suffix.ToString().Length - 4, 4))"
    description = "Department of Information Science and Engineering"
} | ConvertTo-Json
$deptResp = Test-API -Name "ADMIN creates department" -Method POST -Url "$BASE/api/departments" -Body $deptBody -Token $ADMIN_TOKEN -ExpectedStatus 201 -ExpectContains "Information Science"
$dept = $deptResp | ConvertFrom-Json
$DEPT_ID = $dept.id
Write-Host "  Department ID: $DEPT_ID" -ForegroundColor Gray

Test-API -Name "Duplicate department name → 409" -Method POST -Url "$BASE/api/departments" -Body $deptBody -Token $ADMIN_TOKEN -ExpectedStatus 409 -ExpectContains "Information Science"
Test-API -Name "STUDENT cannot create department (403)" -Method POST -Url "$BASE/api/departments" -Body $deptBody -Token $STUDENT_TOKEN -ExpectedStatus 403
Test-API -Name "GET all departments" -Method GET -Url "$BASE/api/departments" -Token $STUDENT_TOKEN -ExpectedStatus 200 -ExpectContains "content"
Test-API -Name "GET department by ID" -Method GET -Url "$BASE/api/departments/$DEPT_ID" -Token $STUDENT_TOKEN -ExpectedStatus 200 -ExpectContains "Information Science"
Test-API -Name "GET nonexistent department → 404" -Method GET -Url "$BASE/api/departments/99999" -Token $STUDENT_TOKEN -ExpectedStatus 404

$updateDeptBody = @{ description = "Updated description for ISE department." } | ConvertTo-Json
Test-API -Name "ADMIN updates department" -Method PUT -Url "$BASE/api/departments/$DEPT_ID" -Body $updateDeptBody -Token $ADMIN_TOKEN -ExpectedStatus 200

# ── Courses ───────────────────────────────
Write-Host "`n--- Resource Hub: Courses ---" -ForegroundColor Yellow
$courseBody = @{
    name = "B.E. Information Science $suffix"
    code = "BE-IS-$($suffix.ToString().Substring($suffix.ToString().Length - 4, 4))"
    departmentId = $DEPT_ID
    description = "Four-year undergraduate program in Information Science"
} | ConvertTo-Json
$courseResp = Test-API -Name "ADMIN creates course in department" -Method POST -Url "$BASE/api/courses" -Body $courseBody -Token $ADMIN_TOKEN -ExpectedStatus 201 -ExpectContains "B.E. Information Science"
$course = $courseResp | ConvertFrom-Json
$COURSE_ID = $course.id
Write-Host "  Course ID: $COURSE_ID" -ForegroundColor Gray

Test-API -Name "GET courses filtered by departmentId" -Method GET -Url "$BASE/api/courses?departmentId=$DEPT_ID" -Token $STUDENT_TOKEN -ExpectedStatus 200 -ExpectContains "content"
Test-API -Name "GET course by ID (with dept name)" -Method GET -Url "$BASE/api/courses/$COURSE_ID" -Token $STUDENT_TOKEN -ExpectedStatus 200 -ExpectContains "Information Science"
Test-API -Name "Course with invalid department → 404" -Method POST -Url "$BASE/api/courses" -Body (@{name="Valid Name"; code="X1"; departmentId=99999}|ConvertTo-Json) -Token $ADMIN_TOKEN -ExpectedStatus 404

# ── Subjects ──────────────────────────────
Write-Host "`n--- Resource Hub: Subjects ---" -ForegroundColor Yellow
$subjBody = @{
    name = "Data Structures $suffix"
    code = "DS-$($suffix.ToString().Substring($suffix.ToString().Length - 6, 6))"
    semester = 3
    courseId = $COURSE_ID
    description = "Introduction to data structures and algorithms"
} | ConvertTo-Json
$subjResp = Test-API -Name "ADMIN creates subject in course (sem 3)" -Method POST -Url "$BASE/api/subjects" -Body $subjBody -Token $ADMIN_TOKEN -ExpectedStatus 201 -ExpectContains "Data Structures"
$subj = $subjResp | ConvertFrom-Json
$SUBJ_ID = $subj.id
Write-Host "  Subject ID: $SUBJ_ID" -ForegroundColor Gray

Test-API -Name "GET subjects filtered by courseId" -Method GET -Url "$BASE/api/subjects?courseId=$COURSE_ID" -Token $STUDENT_TOKEN -ExpectedStatus 200 -ExpectContains "content"
Test-API -Name "GET subjects filtered by courseId and semester" -Method GET -Url "$BASE/api/subjects?courseId=$COURSE_ID&semester=3" -Token $STUDENT_TOKEN -ExpectedStatus 200
Test-API -Name "GET subject by ID (with full hierarchy)" -Method GET -Url "$BASE/api/subjects/$SUBJ_ID" -Token $STUDENT_TOKEN -ExpectedStatus 200 -ExpectContains "B.E. Information Science"
Test-API -Name "Subject with invalid course → 404" -Method POST -Url "$BASE/api/subjects" -Body (@{name="Valid Name"; code="X1"; semester=1; courseId=99999}|ConvertTo-Json) -Token $ADMIN_TOKEN -ExpectedStatus 404

# ── Resources ─────────────────────────────
Write-Host "`n--- Resource Hub: Resources ---" -ForegroundColor Yellow
$resBody = @{
    title = "DS Chapter 1 Notes $suffix"
    description = "Complete lecture notes for Chapter 1: Arrays and Linked Lists"
    resourceType = "NOTES"
    url = "https://drive.google.com/file/ds-ch1-notes-$suffix"
    subjectId = $SUBJ_ID
} | ConvertTo-Json
$resResp = Test-API -Name "ADMIN uploads resource (NOTES)" -Method POST -Url "$BASE/api/resources" -Body $resBody -Token $ADMIN_TOKEN -ExpectedStatus 201 -ExpectContains "NOTES"
$res = $resResp | ConvertFrom-Json
$RES_ID = $res.id
Write-Host "  Resource ID: $RES_ID" -ForegroundColor Gray

$qpBody = @{
    title = "2025 Exam Paper $suffix"
    description = "Previous year question paper"
    resourceType = "QUESTION_PAPER"
    url = "https://drive.google.com/file/ds-qp-$suffix"
    subjectId = $SUBJ_ID
} | ConvertTo-Json
$qpResp = Test-API -Name "CLUB_ADMIN uploads question paper" -Method POST -Url "$BASE/api/resources" -Body $qpBody -Token $LEADER_TOKEN -ExpectedStatus 201 -ExpectContains "QUESTION_PAPER"
$qp = $qpResp | ConvertFrom-Json
$QP_ID = $qp.id

Test-API -Name "STUDENT cannot upload resource (403)" -Method POST -Url "$BASE/api/resources" -Body $resBody -Token $STUDENT_TOKEN -ExpectedStatus 403

Test-API -Name "GET all resources" -Method GET -Url "$BASE/api/resources" -Token $STUDENT_TOKEN -ExpectedStatus 200 -ExpectContains "content"
Test-API -Name "GET resources filtered by subjectId" -Method GET -Url "$BASE/api/resources?subjectId=$SUBJ_ID" -Token $STUDENT_TOKEN -ExpectedStatus 200 -ExpectContains "content"
Test-API -Name "GET resources filtered by resourceType=NOTES" -Method GET -Url "$BASE/api/resources?resourceType=NOTES" -Token $STUDENT_TOKEN -ExpectedStatus 200
Test-API -Name "GET resources by type=QUESTION_PAPER" -Method GET -Url "$BASE/api/resources?resourceType=QUESTION_PAPER" -Token $STUDENT_TOKEN -ExpectedStatus 200
Test-API -Name "Search resources by title" -Method GET -Url "$BASE/api/resources?search=Notes" -Token $STUDENT_TOKEN -ExpectedStatus 200
Test-API -Name "GET resource by ID (full hierarchy)" -Method GET -Url "$BASE/api/resources/$RES_ID" -Token $STUDENT_TOKEN -ExpectedStatus 200 -ExpectContains "Data Structures"

# Non-uploader cannot delete
Test-API -Name "Non-uploader (LEADER) cannot delete ADMIN resource (403)" -Method DELETE -Url "$BASE/api/resources/$RES_ID" -Token $LEADER_TOKEN -ExpectedStatus 403

# Update resource
$updateResBody = @{ title = "DS Chapter 1 Notes UPDATED $suffix" } | ConvertTo-Json
Test-API -Name "ADMIN updates own resource" -Method PUT -Url "$BASE/api/resources/$RES_ID" -Body $updateResBody -Token $ADMIN_TOKEN -ExpectedStatus 200 -ExpectContains "UPDATED"

# LEADER deletes their own resource
Test-API -Name "CLUB_ADMIN deletes own resource" -Method DELETE -Url "$BASE/api/resources/$QP_ID" -Token $LEADER_TOKEN -ExpectedStatus 204

# Resource with invalid subject
$badResBody = @{
    title = "Test"; description = "test desc"; resourceType = "NOTES"
    url = "http://test.com"; subjectId = 99999
} | ConvertTo-Json
Test-API -Name "Resource with invalid subjectId → 404" -Method POST -Url "$BASE/api/resources" -Body $badResBody -Token $ADMIN_TOKEN -ExpectedStatus 404

# ── Delete Cleanup ─────────────────────────
Write-Host "`n--- Cleanup: Delete (correct order) ---" -ForegroundColor Yellow

# Cannot delete subject while resource references it (FK constraint → 409)
Test-API -Name "Delete subject with existing resource → 409 (FK constraint)" -Method DELETE -Url "$BASE/api/subjects/$SUBJ_ID" -Token $ADMIN_TOKEN -ExpectedStatus 409 -ExpectContains "Cannot delete"

# Delete in correct leaf-first order
Test-API -Name "ADMIN deletes resource first (leaf)" -Method DELETE -Url "$BASE/api/resources/$RES_ID" -Token $ADMIN_TOKEN -ExpectedStatus 204
Test-API -Name "ADMIN deletes subject after resource removed" -Method DELETE -Url "$BASE/api/subjects/$SUBJ_ID" -Token $ADMIN_TOKEN -ExpectedStatus 204
Test-API -Name "ADMIN deletes course after subject removed" -Method DELETE -Url "$BASE/api/courses/$COURSE_ID" -Token $ADMIN_TOKEN -ExpectedStatus 204
Test-API -Name "ADMIN deletes department after course removed" -Method DELETE -Url "$BASE/api/departments/$DEPT_ID" -Token $ADMIN_TOKEN -ExpectedStatus 204


# ── Phase 1/2/3 Regression ────────────────
Write-Host "`n--- Phase 1/2/3 Regression ---" -ForegroundColor Yellow
Test-API -Name "P1: Health endpoint" -Method GET -Url "$BASE/actuator/health" -ExpectedStatus 200
Test-API -Name "P1: GET /api/users/me" -Method GET -Url "$BASE/api/users/me" -Token $STUDENT_TOKEN -ExpectedStatus 200 -ExpectContains "student1@campus.edu"
Test-API -Name "P2: GET /api/clubs" -Method GET -Url "$BASE/api/clubs" -Token $STUDENT_TOKEN -ExpectedStatus 200 -ExpectContains "content"
Test-API -Name "P3: GET /api/events" -Method GET -Url "$BASE/api/events" -Token $STUDENT_TOKEN -ExpectedStatus 200 -ExpectContains "content"
Test-API -Name "P3: GET /api/users/me/events" -Method GET -Url "$BASE/api/users/me/events" -Token $STUDENT_TOKEN -ExpectedStatus 200 -ExpectContains "content"

# ── Final Summary ─────────────────────────
Write-Host "`n===== PHASE 4 TEST RESULTS =====" -ForegroundColor Cyan
Write-Host "  PASSED: $pass" -ForegroundColor Green
Write-Host "  FAILED: $fail" -ForegroundColor $(if ($fail -gt 0) { "Red" } else { "Green" })
Write-Host "  TOTAL:  $($pass + $fail)" -ForegroundColor Cyan
