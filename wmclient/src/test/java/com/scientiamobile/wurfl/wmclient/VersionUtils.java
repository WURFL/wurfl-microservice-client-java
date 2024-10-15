/**
 * Copyright 2018 Scientiamobile Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
Copyright 2019 ScientiaMobile Inc. http://www.scientiamobile.com

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.scientiamobile.wurfl.wmclient;

/*
 * Simple utility class used to compare version numbers. Used in test only
 * Created by andrea on 08/01/18.
 */
public class VersionUtils {

    // No instance for this
    private VersionUtils(){}

    /**
     *
     * @param ver1 version number
     * @param ver2 another version number
     * @return 0 when ver1 is the same of ver2, -1 when ver1 is older than ver2, 1 if ver2 is older than ver2
     * @throws IllegalArgumentException when ver1 and ver2 have different version patterns
     * @throws NullPointerException when at least one of ver1,ver2 is null
     */
    public static int compareVersionNumbers(String ver1, String ver2){
        String[] ver1Toks = ver1.split("\\.");
        String[] ver2Toks = ver2.split("\\.");

        int tokensToCompare = ver1Toks.length;
        if (ver2Toks.length < tokensToCompare){
            tokensToCompare = ver2Toks.length;
        }

        for (int i=0; i < tokensToCompare; i++){
            if (Integer.parseInt(ver1Toks[i]) == Integer.parseInt(ver2Toks[i])){
                continue;
            }
            else {
                return Integer.parseInt(ver1Toks[i]) < Integer.parseInt(ver2Toks[i]) ? -1 : 1;
            }
        }
        return 0;
    }

}
