package in.yagnyam.proxy.messages.identity;


public enum SubjectFieldEnum {
    subjectIdType(SubjectFieldTypeEnum.STRING),
    nationality(SubjectFieldTypeEnum.STRING),
    aadhaarNumber(SubjectFieldTypeEnum.STRING),
    name(SubjectFieldTypeEnum.STRING),
    gender(SubjectFieldTypeEnum.STRING),
    age(SubjectFieldTypeEnum.NUMBER),
    is18Plus(SubjectFieldTypeEnum.BOOLEAN),
    dateOfBirth(SubjectFieldTypeEnum.DATE);

    private final SubjectFieldTypeEnum type;

    public SubjectFieldTypeEnum getType() {
        return this.type;
    }

    SubjectFieldEnum(SubjectFieldTypeEnum type) {
        this.type = type;
    }
}
