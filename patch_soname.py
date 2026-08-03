import lief
import sys

for abi in ['arm64-v8a', 'armeabi-v7a']:
    path = f'android/build/intermediates/merged_native_libs/debug/out/lib/{abi}/libpenguin.so'
    try:
        lib = lief.parse(path)
        for entry in lib.dynamic_entries:
            if entry.tag == lief.ELF.DynamicEntry.TAG.SONAME:
                print(f'{abi}: Current SONAME = {entry.name}')
                entry.name = 'libpenguin.so'
                print(f'{abi}: Changed SONAME to libpenguin.so')
                break
        lib.write(path)
        print(f'{abi}: Written successfully')
    except Exception as e:
        print(f'{abi}: ERROR - {e}')
        sys.exit(1)