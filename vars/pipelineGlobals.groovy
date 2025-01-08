def getAccountID(String environment){
    switch(environment) { 
        case 'dev': 
            return "816069152585"
        ... 
        case 'qa': 
            return "816069152585"
        ... 
        case 'uat': 
            return "816069152585"
        ... 
        case 'pre-prod': 
            return "816069152585"
        ...
        case 'prod': 
            return "816069152585"
        ...
        default:
            return "nothing"
    }
}