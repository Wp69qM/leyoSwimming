import urllib.request
import os

urls = [
    ("https://prototype-prod-1254106194.cos.ap-beijing.myqcloud.com/calicat/file/2089349648554254336/canvas/image/2089349648554254336.png", "miniapp-user/src/assets/calicat/screenshots/U-ai-assistant-initial.png"),
    ("https://prototype-prod-1254106194.cos.ap-beijing.myqcloud.com/calicat/file/2089349656318578688/canvas/image/2089349656318578688.png", "miniapp-user/src/assets/calicat/screenshots/U-ai-assistant-chat.png"),
    ("https://prototype-prod-1254106194.cos.ap-beijing.myqcloud.com/calicat/file/2089349662396125184/canvas/image/2089349662396125184.png", "miniapp-user/src/assets/calicat/screenshots/U-ai-assistant-loading.png"),
    ("https://prototype-prod-1254106194.cos.ap-beijing.myqcloud.com/calicat/file/2089349665822871552/canvas/image/2089349665822871552.png", "miniapp-user/src/assets/calicat/screenshots/U-ai-assistant-history.png"),
    ("https://prototype-prod-1254106194.cos.ap-beijing.myqcloud.com/calicat/file/2089349670222028800/canvas/image/2089349670222028800.png", "miniapp-user/src/assets/calicat/screenshots/U-ai-assistant-recommend.png"),
]

for url, path in urls:
    try:
        os.makedirs(os.path.dirname(path), exist_ok=True)
        urllib.request.urlretrieve(url, path)
        print(f"Downloaded {path}")
    except Exception as e:
        print(f"Failed {path}: {e}")
