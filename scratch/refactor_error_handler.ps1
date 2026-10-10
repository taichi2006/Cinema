$files = Get-ChildItem -Path "f:\ltw\cinema\src\main\java\com\cinema" -Recurse -Filter "*.java"

foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw
    $changed = $false
    
    if ($content -match "import com\.cinema\.common\.exception\.ErrorHandler;") {
        $content = $content -replace 'import com\.cinema\.common\.exception\.ErrorHandler;\r?\n', ''
        $changed = $true
    }
    
    # Thay thế ErrorHandler.handle(resp, e)
    if ($content -match "ErrorHandler\.handle") {
        $content = $content -replace 'ErrorHandler\.handle\([^,]+,\s*([^)]+)\);', 'throw new ServletException($1);'
        # Nếu throw new ServletException, cần có import jakarta.servlet.ServletException
        if (-not ($content -match "import jakarta\.servlet\.ServletException;")) {
             $content = $content -replace 'import java\.io\.IOException;', "import jakarta.servlet.ServletException;`r`nimport java.io.IOException;"
        }
        $changed = $true
    }

    if ($changed) {
        [IO.File]::WriteAllText($file.FullName, $content)
        Write-Host "Refactored ErrorHandler in $($file.FullName)"
    }
}
