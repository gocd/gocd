
#include <openssl/pkcs7.h>

void print_pkcs7(PKCS7* p7) {
    printf(" | asn1     : %s\n", p7->asn1);
    printf(" | len      : %d\n", p7->length);
    printf(" | state    : %d\n", p7->state);
    printf(" | detached : %d\n", p7->detached);
    printf(" | type     : %d\n", OBJ_nid2obj(p7->type));
}

int main(int argc, char** argv) {
    PKCS7* p7;
    p7 = PKCS7_new();

    printf("--before:\n");
    print_pkcs7(p7);

    PKCS7_free(p7);
    return 0;
}
