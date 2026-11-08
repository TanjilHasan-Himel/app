import os
from PIL import Image, ImageDraw

def create_icons():
    branding_dir = "branding"
    res_dir = os.path.join("app", "src", "main", "res")

    white_glyph_path = os.path.join(branding_dir, "audia-player-icon-white-1024px.png")
    dark_icon_path = os.path.join(branding_dir, "audia-player-icon-dark-1024px.png")

    if not os.path.exists(white_glyph_path):
        print(f"Error: {white_glyph_path} not found")
        return

    white_glyph = Image.open(white_glyph_path).convert("RGBA")
    dark_icon = Image.open(dark_icon_path).convert("RGBA")

    # Crop glyph to its tight bounding box
    bbox = white_glyph.getbbox()
    glyph_cropped = white_glyph.crop(bbox)
    gw, gh = glyph_cropped.size
    print(f"Glyph tight bbox size: {gw}x{gh}")

    # 1. Create audia_player_logo.png (512x512)
    # Dark #090909 background, glyph centered with safe margin for circle / squircle display
    logo_size = 512
    logo_img = Image.new("RGBA", (logo_size, logo_size), (9, 9, 9, 255))
    target_gw = int(logo_size * 0.62)
    target_gh = int(gh * (target_gw / gw))
    glyph_scaled = glyph_cropped.resize((target_gw, target_gh), Image.Resampling.LANCZOS)
    gx = (logo_size - target_gw) // 2
    gy = (logo_size - target_gh) // 2
    logo_img.paste(glyph_scaled, (gx, gy), glyph_scaled)
    
    logo_out = os.path.join(res_dir, "drawable", "audia_player_logo.png")
    logo_img.save(logo_out, "PNG")
    print(f"Saved {logo_out}")

    # 2. Create ic_launcher_foreground.png (432x432 for xxxhdpi adaptive icon foreground)
    # In Android adaptive icon (108dp), the safe zone is inner 72dp (~66.6%)
    fg_size = 432
    fg_img = Image.new("RGBA", (fg_size, fg_size), (0, 0, 0, 0))
    target_fg_w = int(fg_size * 0.58)
    target_fg_h = int(gh * (target_fg_w / gw))
    glyph_fg = glyph_cropped.resize((target_fg_w, target_fg_h), Image.Resampling.LANCZOS)
    fg_x = (fg_size - target_fg_w) // 2
    fg_y = (fg_size - target_fg_h) // 2
    fg_img.paste(glyph_fg, (fg_x, fg_y), glyph_fg)

    fg_drawable_out = os.path.join(res_dir, "drawable", "ic_launcher_foreground.png")
    fg_img.save(fg_drawable_out, "PNG")
    print(f"Saved {fg_drawable_out}")

    # 3. Create legacy launcher icons for all mipmap densities
    # (Square squircle and circular)
    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192
    }

    for folder, size in densities.items():
        target_dir = os.path.join(res_dir, folder)
        os.makedirs(target_dir, exist_ok=True)

        # Square/squircle icon:
        # Base on dark background with squircle corners (or dark_icon resized)
        # Using logo_img scaled with rounded corners
        sq_img = logo_img.resize((size, size), Image.Resampling.LANCZOS)
        # Apply rounded corner mask (radius ~ 22% of size)
        mask = Image.new("L", (size, size), 0)
        draw = ImageDraw.Draw(mask)
        rad = int(size * 0.22)
        draw.rounded_rectangle([(0, 0), (size - 1, size - 1)], radius=rad, fill=255)
        sq_icon = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        sq_icon.paste(sq_img, (0, 0), mask)
        sq_out = os.path.join(target_dir, "ic_launcher.png")
        sq_icon.save(sq_out, "PNG")

        # Round icon:
        circle_mask = Image.new("L", (size, size), 0)
        circle_draw = ImageDraw.Draw(circle_mask)
        circle_draw.ellipse([(0, 0), (size - 1, size - 1)], fill=255)
        round_icon = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        round_icon.paste(sq_img, (0, 0), circle_mask)
        round_out = os.path.join(target_dir, "ic_launcher_round.png")
        round_icon.save(round_out, "PNG")

        print(f"Saved {sq_out} and {round_out} ({size}x{size})")

    # 4. Create vector notification icon (ic_audia_notification.xml and icon.xml)
    vector_xml = '''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="512"
    android:viewportHeight="512">
    <!-- Audia Triple Arch Brand Mark -->
    <path
        android:pathData="M68,405 L214,117 Q256,35 298,117 L444,405"
        android:strokeColor="#FFFFFFFF"
        android:strokeWidth="38"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
    <path
        android:pathData="M145,405 L226,242 Q256,182 286,242 L367,405"
        android:strokeColor="#FFFFFFFF"
        android:strokeWidth="38"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
    <path
        android:pathData="M222,405 L244,358 Q256,334 268,358 L290,405"
        android:strokeColor="#FFFFFFFF"
        android:strokeWidth="38"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
</vector>
'''
    notif_out = os.path.join(res_dir, "drawable", "ic_audia_notification.xml")
    with open(notif_out, "w", encoding="utf-8") as f:
        f.write(vector_xml)
    print(f"Saved {notif_out}")

    # Also overwrite icon.xml with the clean vector icon so all R.drawable.icon references work
    icon_out = os.path.join(res_dir, "drawable", "icon.xml")
    with open(icon_out, "w", encoding="utf-8") as f:
        f.write(vector_xml)
    print(f"Saved {icon_out}")

    # 5. Clean up illegal raw files from res/drawable/
    illegal_files = [
        "audia-player-icon-black-1024px.png",
        "audia-player-icon-dark-1024px.png",
        "audia-player-icon-light-1024px.png",
        "audia-player-icon-light.svg",
        "audia-player-icon-white-1024px.png",
        "audia-player-light-windows.ico"
    ]
    for fn in illegal_files:
        fp = os.path.join(res_dir, "drawable", fn)
        if os.path.exists(fp):
            os.remove(fp)
            print(f"Removed illegal raw resource: {fn}")

    print("All icons successfully generated and cleaned up!")

if __name__ == "__main__":
    create_icons()

