from PIL import Image
import numpy as np

for mode in ['sun', 'moon']:
    base_path = f'src/main/resources/assets/ironhold/textures/item/soluna_staff_{mode}.png'
    glow_path = f'src/main/resources/assets/ironhold/textures/item/soluna_staff_{mode}_glowmask.png'
    
    img = Image.open(base_path).convert('RGBA')
    arr = np.array(img)
    
    h, w, c = arr.shape
    glow = np.zeros((h, w, 4), dtype=np.uint8)
    
    # The cube is in the top-left 2x2 area!
    for y in range(2):
        for x in range(2):
            glow[y, x] = arr[y, x]
            
    Image.fromarray(glow).save(glow_path)
    print(f"Saved {glow_path}")
