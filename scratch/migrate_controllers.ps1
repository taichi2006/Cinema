$files = Get-ChildItem -Path "f:\ltw\cinema\src\main\java\com\cinema" -Recurse -Filter "*Controller.java"

foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw
    $changed = $false
    
    # Kế thừa BaseServlet
    if ($content -match "extends HttpServlet" -and -not ($content -match "extends BaseServlet")) {
        $content = $content -replace "extends HttpServlet", "extends BaseServlet"
        if (-not ($content -match "import com\.cinema\.common\.web\.BaseServlet;")) {
            $content = $content -replace "import jakarta\.servlet\.http\.HttpServlet;", "import jakarta.servlet.http.HttpServlet;`r`nimport com.cinema.common.web.BaseServlet;"
        }
        $changed = $true
    }

    # Bỏ các dòng rác
    if ($content -match 'response\.setContentType\("application/json;charset=UTF-8"\);') {
        $content = $content -replace 'response\.setContentType\("application/json;charset=UTF-8"\);\r?\n\s*', ""
        $changed = $true
    }
    if ($content -match 'request\.setCharacterEncoding\("UTF-8"\);') {
        $content = $content -replace 'request\.setCharacterEncoding\("UTF-8"\);\r?\n\s*', ""
        $changed = $true
    }
    
    # Bỏ khai báo ObjectMapper
    if ($content -match 'private final ObjectMapper \w+ = new ObjectMapper\(\);') {
        $content = $content -replace 'private final ObjectMapper \w+ = new ObjectMapper\(\);\r?\n\s*', ""
        $changed = $true
    }
    if ($content -match 'import com\.fasterxml\.jackson\.databind\.ObjectMapper;') {
        $content = $content -replace 'import com\.fasterxml\.jackson\.databind\.ObjectMapper;\r?\n', ""
        $changed = $true
    }

    # Thay thế đọc ghi JSON
    # objectMapper.writeValue(response.getWriter(), data) -> sendJson(response, data)
    if ($content -match '\w+\.writeValue\(([^,]+)\.getWriter\(\),\s*([^)]+)\);') {
        $content = $content -replace '\w+\.writeValue\(([^,]+)\.getWriter\(\),\s*([^)]+)\);', 'sendJson($1, $2);'
        $changed = $true
    }
    # objectMapper.readValue(request.getInputStream(), clazz) -> parseBody(request, clazz)
    if ($content -match '\w+\.readValue\(([^,]+)\.getInputStream\(\),\s*([^)]+)\)') {
        $content = $content -replace '\w+\.readValue\(([^,]+)\.getInputStream\(\),\s*([^)]+)\)', 'parseBody($1, $2)'
        $changed = $true
    }

    if ($changed) {
        [IO.File]::WriteAllText($file.FullName, $content)
        Write-Host "Migrated $($file.FullName) to BaseServlet"
    }
}
