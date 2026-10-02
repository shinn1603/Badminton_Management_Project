$connStr = "Server=localhost;Database=badminton;User Id=sa;Password=123456;TrustServerCertificate=True;";
$conn = New-Object System.Data.SqlClient.SqlConnection($connStr);
$conn.Open();

$sqlScript = [System.IO.File]::ReadAllText("$PSScriptRoot\..\database\init_badminton.sql", [System.Text.Encoding]::UTF8);
$batches = $sqlScript -split '(?m)^\s*GO\s*$'

foreach ($batch in $batches) {
    $trimmed = $batch.Trim();
    if ($trimmed.Length -gt 0) {
        $cmd = $conn.CreateCommand();
        $cmd.CommandText = $trimmed;
        $cmd.ExecuteNonQuery() | Out-Null;
    }
}
$conn.Close();
Write-Host "SQL Server Database re-initialized cleanly with UTF-8 and Badminton only!";
