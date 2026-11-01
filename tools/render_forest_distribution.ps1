# Plot the actual surface-climate samples exported by ForestDistributionGameTests.
# Uses Windows' existing System.Drawing runtime; no downloads or Python packages.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$dataDirectory = Join-Path (Split-Path $PSScriptRoot -Parent) 'build/forest-distribution-audit'
$palette = @('#45664b', '#d4d6ca', '#ef9539', '#397ab0', '#214c42', '#203925') |
    ForEach-Object { [System.Drawing.ColorTranslator]::FromHtml($_) }
$font = [System.Drawing.Font]::new('Arial', 15)
$titleFont = [System.Drawing.Font]::new('Arial', 20, [System.Drawing.FontStyle]::Bold)
$smallFont = [System.Drawing.Font]::new('Arial', 12)
$textBrush = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml('#17291f'))
try {
    foreach ($seed in @(0, 2026, 271727)) {
        $canvas = [System.Drawing.Bitmap]::new(1120, 660)
        $graphics = [System.Drawing.Graphics]::FromImage($canvas)
        try {
            $graphics.Clear([System.Drawing.ColorTranslator]::FromHtml('#f5f4ef'))
            $graphics.DrawString("Forest distribution | Seed $seed", $titleFont, $textBrush, 24, 14)
            $graphics.DrawString('Surface climate samples: 8,192 x 8,192 blocks; spacing: 32 blocks', $smallFont, $textBrush, 24, 50)
            $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
            $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
            $index = 0
            foreach ($phase in @('before', 'after')) {
                $rows = @(Get-Content -LiteralPath (Join-Path $dataDirectory "seed-$seed-$phase.csv"))
                $grid = [System.Drawing.Bitmap]::new(257, 257)
                try {
                    for ($z = 0; $z -lt $rows.Length; $z++) {
                        $cells = $rows[$z].Split(',')
                        for ($x = 0; $x -lt $cells.Length; $x++) {
                            $grid.SetPixel($x, $z, $palette[[int]$cells[$x]])
                        }
                    }
                    $panelX = 24 + $index * 554
                    $label = if ($phase -eq 'before') { 'Before: per-cell weirdness strips' } else { 'After: continuous climate windows' }
                    $graphics.DrawString($label, $font, $textBrush, $panelX, 82)
                    $graphics.DrawImage($grid, [System.Drawing.Rectangle]::new($panelX, 112, 514, 514))
                } finally { $grid.Dispose() }
                $index++
            }
            $labels = @('Pale Garden', 'Dappled Forest', 'Water', 'Taiga', 'Dark Forest', 'Other')
            $positions = @(24, 232, 464, 622, 765, 957)
            $colors = @(1, 2, 3, 4, 5, 0)
            for ($i = 0; $i -lt $labels.Length; $i++) {
                $brush = [System.Drawing.SolidBrush]::new($palette[$colors[$i]])
                try { $graphics.FillRectangle($brush, $positions[$i], 637, 14, 14) } finally { $brush.Dispose() }
                $graphics.DrawString($labels[$i], $smallFont, $textBrush, ($positions[$i] + 20), 635)
            }
            $destination = Join-Path $dataDirectory "seed-$seed-comparison.png"
            $canvas.Save($destination, [System.Drawing.Imaging.ImageFormat]::Png)
            Write-Output $destination
        } finally { $graphics.Dispose(); $canvas.Dispose() }
    }
} finally {
    $font.Dispose(); $titleFont.Dispose(); $smallFont.Dispose(); $textBrush.Dispose()
}
