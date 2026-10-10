$files = Get-ChildItem -Path "f:\ltw\cinema\src\main\java\com\cinema" -Recurse -Filter "*.java"

foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw
    if ($content -match "CommonDTO") {
        # Thay thế import
        $content = $content -replace 'import com\.cinema\.common\.dto\.CommonDTO;', "import com.cinema.common.dto.response.ApiResponse;`r`nimport com.cinema.common.dto.response.ErrorResponse;`r`nimport com.cinema.common.dto.response.PageMeta;`r`nimport com.cinema.common.dto.response.PageResponse;"
        
        # Thay thế các class con
        $content = $content -replace 'CommonDTO\.ApiResponse', 'ApiResponse'
        $content = $content -replace 'CommonDTO\.PageMeta', 'PageMeta'
        $content = $content -replace 'CommonDTO\.ErrorResponse', 'ErrorResponse'
        $content = $content -replace 'CommonDTO\.PageResponse', 'PageResponse'

        [IO.File]::WriteAllText($file.FullName, $content)
        Write-Host "Updated $($file.FullName)"
    }
}
