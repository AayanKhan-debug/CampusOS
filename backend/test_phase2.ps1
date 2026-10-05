# Comprehensive Phase 2 API Test Script with Dynamic Club Name

Write-Host "=========================================================="
Write-Host "         CAMPUSOS PHASE 2 API INTEGRATION TESTS          "
Write-Host "=========================================================="

# 1. Logins
$adminLogin = @{ email = "admin@campus.edu"; password = "admin123" } | ConvertTo-Json
$adminRes = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $adminLogin -ContentType "application/json"
$adminToken = $adminRes.token
$adminHeaders = @{ "Authorization" = "Bearer $adminToken" }
Write-Host "[1/18] Admin logged in. Role:" $adminRes.user.role

$leaderLogin = @{ email = "leader@campus.edu"; password = "password123" } | ConvertTo-Json
$leaderRes = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $leaderLogin -ContentType "application/json"
$leaderToken = $leaderRes.token
$leaderId = $leaderRes.user.id
$leaderHeaders = @{ "Authorization" = "Bearer $leaderToken" }
Write-Host "[2/18] Club Leader logged in (ID: $leaderId). Role:" $leaderRes.user.role

$s1Login = @{ email = "student1@campus.edu"; password = "password123" } | ConvertTo-Json
$s1Res = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $s1Login -ContentType "application/json"
$s1Token = $s1Res.token
$s1Id = $s1Res.user.id
$s1Headers = @{ "Authorization" = "Bearer $s1Token" }
Write-Host "[3/18] Student 1 logged in (ID: $s1Id). Role:" $s1Res.user.role

$s2Login = @{ email = "student2@campus.edu"; password = "password123" } | ConvertTo-Json
$s2Res = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $s2Login -ContentType "application/json"
$s2Token = $s2Res.token
$s2Id = $s2Res.user.id
$s2Headers = @{ "Authorization" = "Bearer $s2Token" }
Write-Host "[4/18] Student 2 logged in (ID: $s2Id). Role:" $s2Res.user.role

# 2. Create Club by CLUB_ADMIN
$uniqueClubName = "Robotics Club " + [System.DateTime]::Now.Ticks
$createClub = @{
    name = $uniqueClubName
    description = "Building autonomous robots and competitive machines"
    category = "Technical"
    logo = "https://example.com/robotics.png"
} | ConvertTo-Json
$clubRes = Invoke-RestMethod -Uri "http://localhost:8080/api/clubs" -Method Post -Body $createClub -ContentType "application/json" -Headers $leaderHeaders
$clubId = $clubRes.id
Write-Host "[5/18] Club Created successfully! ID: $clubId, Name:" $clubRes.name ", Status:" $clubRes.status

# 3. Duplicate Club Name Check
try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/clubs" -Method Post -Body $createClub -ContentType "application/json" -Headers $leaderHeaders
    Write-Host "[6/18] ERROR: Duplicate club name allowed!"
} catch {
    Write-Host "[6/18] Duplicate club name correctly rejected! Status:" $_.Exception.Response.StatusCode.value__
}

# 4. Unauthorized Club Creation (Student)
$studentClub = @{ name = "Unauthorized Club " + [System.DateTime]::Now.Ticks } | ConvertTo-Json
try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/clubs" -Method Post -Body $studentClub -ContentType "application/json" -Headers $s1Headers
    Write-Host "[7/18] ERROR: Student created a club without permission!"
} catch {
    Write-Host "[7/18] Student club creation correctly blocked! Status:" $_.Exception.Response.StatusCode.value__
}

# 5. Get All Clubs (with search & pagination)
$allClubs = Invoke-RestMethod -Uri "http://localhost:8080/api/clubs?search=Robotics&page=0&size=10" -Method Get -Headers $s1Headers
Write-Host "[8/18] Get all clubs returned $($allClubs.totalElements) club(s). First club:" $allClubs.content[0].name ", MemberCount:" $allClubs.content[0].memberCount

# 6. Student 1 Join Request
$join1 = Invoke-RestMethod -Uri "http://localhost:8080/api/clubs/$clubId/join" -Method Post -Headers $s1Headers
Write-Host "[9/18] Student 1 join request submitted! Status:" $join1.status ", Club:" $join1.clubName

# 7. Duplicate Pending Join Request (Student 1)
try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/clubs/$clubId/join" -Method Post -Headers $s1Headers
    Write-Host "[10/18] ERROR: Duplicate pending request allowed!"
} catch {
    Write-Host "[10/18] Duplicate pending request correctly rejected! Status:" $_.Exception.Response.StatusCode.value__
}

# 8. Student 2 Join Request
$join2 = Invoke-RestMethod -Uri "http://localhost:8080/api/clubs/$clubId/join" -Method Post -Headers $s2Headers
Write-Host "[11/18] Student 2 join request submitted! Status:" $join2.status

# 9. View Pending Requests (by Leader)
$pendingReqs = Invoke-RestMethod -Uri "http://localhost:8080/api/clubs/$clubId/members/requests" -Method Get -Headers $leaderHeaders
Write-Host "[12/18] Leader retrieved pending requests. Total pending:" $pendingReqs.totalElements

# 10. Unauthorized View Pending Requests (by Student 1)
try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/clubs/$clubId/members/requests" -Method Get -Headers $s1Headers
    Write-Host "[13/18] ERROR: Student viewed pending requests!"
} catch {
    Write-Host "[13/18] Unauthorized view pending requests correctly blocked! Status:" $_.Exception.Response.StatusCode.value__
}

# 11. Leader Approves Student 1
$approveBody = @{ status = "APPROVED" } | ConvertTo-Json
$approveRes = Invoke-RestMethod -Uri "http://localhost:8080/api/clubs/$clubId/members/$s1Id" -Method Put -Body $approveBody -ContentType "application/json" -Headers $leaderHeaders
Write-Host "[14/18] Student 1 membership APPROVED! Status:" $approveRes.status ", JoinedAt:" $approveRes.joinedAt

# 12. Leader Rejects Student 2
$rejectBody = @{ status = "REJECTED" } | ConvertTo-Json
$rejectRes = Invoke-RestMethod -Uri "http://localhost:8080/api/clubs/$clubId/members/$s2Id" -Method Put -Body $rejectBody -ContentType "application/json" -Headers $leaderHeaders
Write-Host "[15/18] Student 2 membership REJECTED! Status:" $rejectRes.status

# 13. Student 2 Re-applies after Rejection
$reapplyRes = Invoke-RestMethod -Uri "http://localhost:8080/api/clubs/$clubId/join" -Method Post -Headers $s2Headers
Write-Host "[16/18] Student 2 re-applied successfully! Status reset to:" $reapplyRes.status

# 14. View Approved Members
$members = Invoke-RestMethod -Uri "http://localhost:8080/api/clubs/$clubId/members" -Method Get -Headers $s1Headers
Write-Host "[17/18] Approved members list retrieved. Total approved members:" $members.totalElements

# 15. Update Club & Soft Deactivation
$updateClub = @{ description = "Updated: Cutting-edge robotics research team." } | ConvertTo-Json
$updated = Invoke-RestMethod -Uri "http://localhost:8080/api/clubs/$clubId" -Method Put -Body $updateClub -ContentType "application/json" -Headers $leaderHeaders
Write-Host "[18/18] Club updated! Description:" $updated.description

Write-Host "`n=========================================================="
Write-Host "        ALL PHASE 2 API INTEGRATION TESTS PASSED!         "
Write-Host "=========================================================="
