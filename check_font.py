with open('core/src/main/assets/fonts/NotoSansKR-Regular.ttf', 'rb') as f:
    data = f.read(200)
    print(f'Size: {len(data)} bytes')
    print(data[:200])