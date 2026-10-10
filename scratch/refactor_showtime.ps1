$replacements = @(
    @{
        File = "f:\ltw\cinema\src\main\java\com\cinema\showtime\ShowtimeService.java"
        Find = "public Map<String, Object> getSeatMap"
        Replace = "public SeatMapResponse getSeatMap"
    },
    @{
        File = "f:\ltw\cinema\src\main\java\com\cinema\showtime\ShowtimeService.java"
        Find = "Map<String, Object> result = new LinkedHashMap<>();`r`n        result.put(`"success`", true);`r`n        result.put(`"data`", seatMap);`r`n        result.put(`"meta`", Collections.emptyMap());`r`n        return result;"
        Replace = "return seatMap;"
    },
    @{
        File = "f:\ltw\cinema\src\main\java\com\cinema\showtime\ShowtimeService.java"
        Find = "public Map<String, Object> getShowtimeDetail"
        Replace = "public ShowtimeDetailResponse getShowtimeDetail"
    },
    @{
        File = "f:\ltw\cinema\src\main\java\com\cinema\showtime\ShowtimeService.java"
        Find = "Map<String, Object> result = new LinkedHashMap<>();`r`n        result.put(`"success`", true);`r`n        result.put(`"data`", showtimeDetail);`r`n        result.put(`"meta`", Collections.emptyMap());`r`n        return result;"
        Replace = "return showtimeDetail;"
    },
    @{
        File = "f:\ltw\cinema\src\main\java\com\cinema\showtime\ShowtimeController.java"
        Find = "Map<String, Object> result = showtimeService.getShowtimeDetail(id);`r`n                response.setStatus(HttpServletResponse.SC_OK);`r`n                sendJson(response, result);"
        Replace = "ShowtimeDetailResponse result = showtimeService.getShowtimeDetail(id);`r`n                writeSuccess(response, result);"
    },
    @{
        File = "f:\ltw\cinema\src\main\java\com\cinema\showtime\ShowtimeController.java"
        Find = "Map<String, Object> result = showtimeService.getSeatMap(id, userId);`r`n`r`n        response.setStatus(HttpServletResponse.SC_OK);`r`n        sendJson(response, result);"
        Replace = "SeatMapResponse result = showtimeService.getSeatMap(id, userId);`r`n        writeSuccess(response, result);"
    }
)

foreach ($r in $replacements) {
    if (Test-Path $r.File) {
        $content = Get-Content $r.File -Raw
        $content = $content.Replace($r.Find, $r.Replace)
        [IO.File]::WriteAllText($r.File, $content)
    }
}
Write-Host "Refactored Showtime"
